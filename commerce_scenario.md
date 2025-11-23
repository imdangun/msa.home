# Commerce 멀티모듈 MSA 시나리오

## 프로젝트 구조
```
commerce/
├── build.gradle.kts (root)
├── settings.gradle.kts
├── common/           # 공통 모듈 (DTO, Entity, 공통 설정)
│   ├── build.gradle.kts
│   └── src/main/java/com/msa/common/
│       ├── dto/
│       │   ├── ProductDTO.java
│       │   ├── OrderDTO.java
│       │   └── OrderItemDTO.java
│       ├── entity/
│       │   ├── BaseEntity.java
│       │   └── OrderStatus.java
│       └── exception/
│           └── ApiException.java
├── product/         # 상품 서비스 (상품/재고 관리)
│   ├── build.gradle.kts
│   └── src/main/java/com/msa/product/
│       ├── ProductServiceApplication.java (Port: 8081)
│       ├── controller/
│       │   └── ProductController.java
│       ├── service/
│       │   └── ProductService.java
│       ├── repository/
│       │   └── ProductRepository.java
│       └── entity/
│           └── Product.java
└── order/          # 주문 서비스 (주문 관리)
    ├── build.gradle.kts
    └── src/main/java/com/msa/order/
        ├── OrderServiceApplication.java (Port: 8082)
        ├── controller/
        │   └── OrderController.java
        ├── service/
        │   ├── OrderService.java
        │   └── OrderSagaOrchestrator.java
        ├── client/
        │   └── ProductClient.java (OpenFeign)
        ├── repository/
        │   └── OrderRepository.java
        └── entity/
            ├── Order.java
            └── OrderItem.java
```

---

## 시나리오: 주문 생성 프로세스

### 1️⃣ 사용자가 주문 생성 요청
```
POST /api/orders
Content-Type: application/json

{
  "customerId": "CUST001",
  "items": [
    {
      "productId": "PROD001",
      "quantity": 2,
      "price": 15000
    }
  ],
  "totalAmount": 30000
}
```

**뜻풀이:**
- 클라이언트(사용자)가 Order Service의 `/api/orders` 엔드포인트로 POST 요청을 보냅니다
- `customerId`: 주문을 하는 고객의 ID
- `items`: 주문에 포함된 상품 목록 (상품ID, 수량, 가격)
- `totalAmount`: 주문 총액

---

### 2️⃣ Order Service의 처리 흐름

#### **Step 1: 주문 정보 수신**
```
OrderController.createOrder(OrderDTO dto)
  ↓
OrderService.createOrder(OrderDTO dto)
  ↓
OrderSagaOrchestrator.executeOrderSaga(OrderDTO dto)
```

**뜻풀이:**
- 클라이언트 요청이 OrderController의 `createOrder` 메서드로 들어옵니다
- OrderService가 실제 비즈니스 로직을 처리하도록 위임합니다
- OrderSagaOrchestrator가 분산 트랜잭션(Saga 패턴)을 조율하기 시작합니다

---

#### **Step 2: Order 엔티티 생성 (상태: PENDING)**
```java
Order order = Order.builder()
    .customerId(dto.getCustomerId())
    .totalAmount(dto.getTotalAmount())
    .status(OrderStatus.PENDING)
    .items(new ArrayList<>())
    .build();

order = orderRepository.save(order);
```

**뜻풀이:**
- Order 객체를 **Builder 패턴**으로 생성합니다
- `customerId`: 주문한 고객 ID를 저장합니다
- `totalAmount`: 주문 총액을 저장합니다
- `status`: OrderStatus.PENDING 상태로 설정합니다 (아직 확정되지 않은 상태)
- `items`: 빈 ArrayList로 초기화합니다 (나중에 주문 항목들을 추가)
- `orderRepository.save(order)`: 데이터베이스에 주문 정보를 저장합니다

이 단계에서는 **아직 상품 재고를 차감하지 않았습니다** (상품 서비스 호출 전)

---

#### **Step 3: 주문 항목들을 OrderItem으로 변환하여 Order에 추가**
```java
for (OrderItemDTO itemDTO : dto.getItems()) {
    OrderItem item = OrderItem.builder()
        .productId(itemDTO.getProductId())
        .quantity(itemDTO.getQuantity())
        .price(itemDTO.getPrice())
        .order(order)
        .build();
    order.getItems().add(item);
}
```

**뜻풀이:**
- 요청에 포함된 각 상품 정보를 **OrderItem 엔티티**로 변환합니다
- `productId`: 상품 ID
- `quantity`: 구매 수량
- `price`: 상품 가격
- `order`: 이 OrderItem이 속한 Order를 참조합니다 (양방향 관계 설정)
- `order.getItems().add(item)`: Order의 items 리스트에 OrderItem을 추가합니다

---

#### **Step 4: OpenFeign을 통해 Product Service 호출 (재고 차감)**
```java
for (OrderItem item : order.getItems()) {
    log.info("재고 차감 요청: 상품 ID={}, 수량={}", item.getProductId(), item.getQuantity());
    productClient.decreaseStock(item.getProductId(), item.getQuantity());
    log.info("재고 차감 성공: 상품 ID={}", item.getProductId());
}
```

**뜻풀이:**
- `productClient`: OpenFeign 클라이언트 (Product Service를 호출하는 인터페이스)
- `decreaseStock(productId, quantity)`: 특정 상품의 재고를 감소시키는 메서드를 호출합니다
- 각 상품마다 Product Service에 HTTP 요청을 보냅니다
- **이 단계가 실패하면 보상 트랜잭션이 실행됩니다** (나중에 설명)

**Product Service의 동작:**
```
Product Service에서:
1. 해당 상품을 데이터베이스에서 조회
2. 현재 재고에서 수량을 차감 (quantity -= 2)
3. 재고가 부족하면 예외 발생
4. 수정된 상품 정보를 데이터베이스에 저장
5. Order Service로 응답 반환
```

---

#### **Step 5: Order 상태 변경 (PENDING → COMPLETED)**
```java
order.setStatus(OrderStatus.COMPLETED);
order = orderRepository.save(order);
log.info("Step 3 완료: Order 상태 변경 (COMPLETED)");
```

**뜻풀이:**
- Product Service의 재고 차감이 **성공했으므로**, Order의 상태를 COMPLETED로 변경합니다
- 이제 주문은 **확정된 상태**입니다
- 변경된 상태를 데이터베이스에 저장합니다

---

#### **Step 6: 성공 응답 반환**
```java
return toDTO(order);
```

**뜻풀이:**
- 생성된 Order 엔티티를 OrderDTO로 변환하여 클라이언트에게 반환합니다
- 클라이언트는 주문 ID, 상태(COMPLETED), 주문 시간 등의 정보를 받습니다

---

### 3️⃣ 실패 시나리오: 보상 트랜잭션 (Compensating Transaction)

#### **상황: Product Service에서 재고 부족 예외 발생**
```java
catch (FeignException.BadRequest e) {
    log.error("재고 부족: {}", e.getMessage());
    compensate(order);
    throw new ApiException("재고가 부족합니다", "INSUFFICIENT_STOCK", 400);
}
```

**뜻풀이:**
- Product Service에서 `BadRequest` 예외를 반환했습니다
- 이는 "요청하신 수량의 재고가 없습니다"라는 의미입니다
- `compensate(order)` 메서드를 호출하여 이미 실행된 작업을 되돌립니다

---

#### **보상 트랜잭션: 재고 복구**
```java
private void compensate(Order order) {
    log.warn("=== 보상 트랜잭션 시작 (Order ID: {}) ===", order.getId());
    
    try {
        // Product Service에 재고 복구 요청
        for (OrderItem item : order.getItems()) {
            log.info("재고 복구 요청: 상품 ID={}, 수량={}", item.getProductId(), item.getQuantity());
            productClient.increaseStock(item.getProductId(), item.getQuantity());
            log.info("재고 복구 완료: 상품 ID={}", item.getProductId());
        }
        
        // Order 상태를 FAILED로 변경
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        log.warn("Order 상태 변경 (FAILED)");
    } catch (Exception e) {
        log.error("보상 트랜잭션 중 오류 발생: {}", e.getMessage());
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
    }
}
```

**뜻풀이:**

**Part 1: 재고 복구 요청**
- Product Service의 `increaseStock` 메서드를 호출합니다
- 이전에 차감했던 재고를 다시 원래대로 복구합니다
- 예: 2개를 차감했으면, 2개를 다시 추가합니다

**Part 2: Order 상태를 FAILED로 변경**
- 주문을 FAILED(실패) 상태로 표시합니다
- 이 주문은 완료되지 않았음을 나타냅니다

**Part 3: 보상 트랜잭션 자체가 실패한 경우**
- 재고 복구도 실패하면? (네트워크 오류 등)
- 그래도 Order를 FAILED 상태로 업데이트합니다
- 로그에 기록하고 관리자가 수동으로 개입하도록 합니다

---

### 4️⃣ 전체 흐름도 (성공 vs 실패)

#### **✅ 성공 시나리오**
```
클라이언트
    ↓
[주문 생성 요청]
    ↓
Order 생성 (PENDING)
    ↓
Product Service 호출
[재고 차감 성공 ✅]
    ↓
Order 상태 변경 (COMPLETED)
    ↓
[주문 완료 응답 반환]
```

#### **❌ 실패 시나리오**
```
클라이언트
    ↓
[주문 생성 요청]
    ↓
Order 생성 (PENDING)
    ↓
Product Service 호출
[재고 부족 ❌ 예외 발생]
    ↓
[보상 트랜잭션 실행]
  - 재고 복구 요청
  - Order.status = FAILED
    ↓
[실패 응답 반환]
```

---

### 5️⃣ Saga 패턴의 장점

**분산 트랜잭션을 안전하게 처리:**
- 각 서비스가 독립적으로 작동하면서도
- 한쪽이 실패하면 다른 쪽도 자동으로 되돌립니다

**데이터 일관성 유지:**
- 재고 부족 시: 주문도 생성되지 않고 (실패 상태) + 재고도 변경되지 않습니다
- 결과: 주문-재고 정보가 항상 일치합니다

**서비스 독립성:**
- Order Service가 Product Service의 데이터베이스에 직접 접근하지 않습니다
- 오직 API(OpenFeign)를 통해서만 통신합니다

---

## 코드 예제

### common 모듈

#### src/main/java/com/msa/common/dto/ProductDTO.java
```java
package com.msa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private String id;
    private String name;
    private Long price;
    private Long quantity;
    private String description;
}
```

#### src/main/java/com/msa/common/dto/OrderItemDTO.java
```java
package com.msa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private String productId;
    private Long quantity;
    private Long price;
}
```

#### src/main/java/com/msa/common/dto/OrderDTO.java
```java
package com.msa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private String id;
    private String customerId;
    private List<OrderItemDTO> items;
    private Long totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### src/main/java/com/msa/common/entity/OrderStatus.java
```java
package com.msa.common.entity;

public enum OrderStatus {
    PENDING,      // 주문 대기
    CONFIRMED,    // 주문 확인
    COMPLETED,    // 주문 완료
    CANCELLED,    // 주문 취소
    FAILED        // 주문 실패
}
```

#### src/main/java/com/msa/common/entity/BaseEntity.java
```java
package com.msa.common.entity;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

#### src/main/java/com/msa/common/exception/ApiException.java
```java
package com.msa.common.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final int status;
    
    public ApiException(String message, String code, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
```

### product 모듈

#### src/main/java/com/msa/product/entity/Product.java
```java
package com.msa.product.entity;

import com.msa.common.entity.BaseEntity;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private Long price;
    
    @Column(nullable = false)
    private Long quantity;
    
    @Column(length = 500)
    private String description;
    
    // 재고 차감
    public void decreaseQuantity(Long amount) {
        if (this.quantity < amount) {
            throw new IllegalArgumentException("재고 부족: " + this.name);
        }
        this.quantity -= amount;
    }
    
    // 재고 증가 (취소 시)
    public void increaseQuantity(Long amount) {
        this.quantity += amount;
    }
}
```

#### src/main/java/com/msa/product/repository/ProductRepository.java
```java
package com.msa.product.repository;

import com.msa.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
    // 자동으로 제공되는 메서드 외에 필요시 커스텀 쿼리 추가
}
```

#### src/main/java/com/msa/product/service/ProductService.java
```java
package com.msa.product.service;

import com.msa.common.dto.ProductDTO;
import com.msa.common.exception.ApiException;
import com.msa.product.entity.Product;
import com.msa.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public ProductDTO getProduct(String productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ApiException("상품을 찾을 수 없습니다", "PRODUCT_NOT_FOUND", 404));
        
        return toDTO(product);
    }
    
    // 주문 서비스에서 호출됨
    public void decreaseStock(String productId, Long quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ApiException("상품을 찾을 수 없습니다", "PRODUCT_NOT_FOUND", 404));
        
        product.decreaseQuantity(quantity);
        productRepository.save(product);
    }
    
    // 주문 취소 시 호출됨
    public void increaseStock(String productId, Long quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ApiException("상품을 찾을 수 없습니다", "PRODUCT_NOT_FOUND", 404));
        
        product.increaseQuantity(quantity);
        productRepository.save(product);
    }
    
    public ProductDTO createProduct(ProductDTO dto) {
        Product product = Product.builder()
            .name(dto.getName())
            .price(dto.getPrice())
            .quantity(dto.getQuantity())
            .description(dto.getDescription())
            .build();
        
        return toDTO(productRepository.save(product));
    }
    
    private ProductDTO toDTO(Product product) {
        return new ProductDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getQuantity(),
            product.getDescription()
        );
    }
}
```

#### src/main/java/com/msa/product/controller/ProductController.java
```java
package com.msa.product.controller;

import com.msa.common.dto.ProductDTO;
import com.msa.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ProductController
 * 
 * 상품 서비스의 REST API를 제공합니다.
 * Order Service는 이 컨트롤러의 엔드포인트를 OpenFeign으로 호출합니다.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    /**
     * GET /api/products/{id}
     * 
     * 특정 상품 정보 조회
     * 
     * @param id 상품 ID
     * @return 상품 정보 (ProductDTO)
     * 
     * 뜻풀이:
     * - @GetMapping("/{id}")는 HTTP GET 요청을 처리합니다
     * - @PathVariable String id는 URL 경로의 {id} 값을 메서드 파라미터로 받습니다
     * - 예: GET /api/products/PROD001 → id = "PROD001"
     * - ResponseEntity.ok()로 200 OK 상태코드와 함께 상품 정보를 반환합니다
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }
    
    /**
     * POST /api/products
     * 
     * 새로운 상품 생성
     * 
     * @param dto 상품 정보 (ProductDTO)
     * @return 생성된 상품 정보
     * 
     * 뜻풀이:
     * - @PostMapping은 HTTP POST 요청을 처리합니다
     * - @RequestBody ProductDTO는 JSON 요청 본문을 ProductDTO 객체로 변환합니다
     * - ResponseEntity.status(HttpStatus.CREATED)로 201 Created 상태코드를 반환합니다
     * - body()로 생성된 상품 정보를 응답 본문에 포함합니다
     * 
     * 요청 예시:
     * POST /api/products
     * {
     *   "name": "노트북",
     *   "price": 1500000,
     *   "quantity": 10,
     *   "description": "고성능 노트북"
     * }
     */
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(productService.createProduct(dto));
    }
    
    /**
     * PUT /api/products/{id}/decrease-stock
     * 
     * 상품의 재고를 감소시킵니다 (Order Service에서 호출)
     * 
     * @param id 상품 ID
     * @param quantity 감소할 수량
     * @return 204 No Content
     * 
     * 뜻풀이:
     * - @PutMapping("/{id}/decrease-stock")은 HTTP PUT 요청을 처리합니다
     * - @RequestParam Long quantity는 URL의 쿼리 파라미터 값을 받습니다
     * - 예: PUT /api/products/PROD001/decrease-stock?quantity=2
     *       → id = "PROD001", quantity = 2
     * - ResponseEntity.ok().build()로 204 No Content를 반환합니다 (응답 본문 없음)
     * 
     * 이 메서드는 Order Service의 OpenFeign 클라이언트에서 호출됩니다:
     * productClient.decreaseStock(productId, quantity)
     * 
     * 호출 장면:
     * Order Service가 주문 생성 시, 각 상품의 재고를 감소시키기 위해
     * 이 엔드포인트를 호출합니다
     */
    @PutMapping("/{id}/decrease-stock")
    public ResponseEntity<Void> decreaseStock(
        @PathVariable String id,
        @RequestParam Long quantity) {
        productService.decreaseStock(id, quantity);
        return ResponseEntity.ok().build();
    }
    
    /**
     * PUT /api/products/{id}/increase-stock
     * 
     * 상품의 재고를 증가시킵니다 (Order Service의 보상 트랜잭션에서 호출)
     * 
     * @param id 상품 ID
     * @param quantity 증가할 수량
     * @return 204 No Content
     * 
     * 뜻풀이:
     * - decreaseStock과 비슷하지만, 재고를 증가시킵니다
     * - 주문 실패 시 보상 트랜잭션에서 이 메서드가 호출됩니다
     * - Order Service의 compensate() 메서드에서 이를 호출합니다
     * 
     * 시나리오 예:
     * 1. 주문 생성 시 노트북 2개 차감: quantity = 10 - 2 = 8
     * 2. 재고 부족으로 실패
     * 3. 보상: 노트북 2개 다시 추가: quantity = 8 + 2 = 10 (원상복구)
     */
    @PutMapping("/{id}/increase-stock")
    public ResponseEntity<Void> increaseStock(
        @PathVariable String id,
        @RequestParam Long quantity) {
        productService.increaseStock(id, quantity);
        return ResponseEntity.ok().build();
    }
}
```

#### src/main/java/com/msa/product/ProductServiceApplication.java
```java
package com.msa.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ProductServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
```

### order 모듈

#### src/main/java/com/msa/order/client/ProductClient.java (OpenFeign)
```java
package com.msa.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ProductClient - OpenFeign 클라이언트
 * 
 * OpenFeign은 선언적 HTTP 클라이언트입니다.
 * 인터페이스만 선언하면, Spring Boot가 자동으로 HTTP 요청/응답 처리를 합니다.
 * 
 * 뜻풀이:
 * - @FeignClient(name = "product-service", url = "http://localhost:8081")
 *   ├─ name: 서비스의 논리적 이름 (Eureka에서 서비스를 찾을 때 사용)
 *   └─ url: Product Service의 실제 주소 (개발 환경에서는 하드코딩, 운영에서는 Eureka 사용)
 * 
 * 이 인터페이스는 직접 구현할 필요가 없습니다.
 * Spring이 프록시 객체를 자동으로 생성하여 @Autowired로 주입받을 수 있습니다.
 */
@FeignClient(name = "product-service", url = "http://localhost:8081")
public interface ProductClient {
    
    /**
     * decreaseStock - 상품 재고 감소
     * 
     * @param id 상품 ID
     * @param quantity 감소할 수량
     * 
     * 뜻풀이:
     * - @PutMapping("/api/products/{id}/decrease-stock")
     *   └─ Product Service의 /api/products/{id}/decrease-stock으로 HTTP PUT 요청을 보냅니다
     * 
     * - @PathVariable String id
     *   └─ 메서드의 id 파라미터를 URL의 {id}로 치환합니다
     *   └─ 예: decreaseStock("PROD001", 2) → PUT /api/products/PROD001/decrease-stock
     * 
     * - @RequestParam Long quantity
     *   └─ 메서드의 quantity 파라미터를 쿼리 파라미터로 전송합니다
     *   └─ 예: ?quantity=2
     * 
     * 실제 HTTP 요청:
     * PUT http://localhost:8081/api/products/PROD001/decrease-stock?quantity=2
     * 
     * 호출 예시:
     * productClient.decreaseStock("PROD001", 2L);
     * 
     * OrderSagaOrchestrator에서의 호출:
     * for (OrderItem item : order.getItems()) {
     *     productClient.decreaseStock(item.getProductId(), item.getQuantity());
     * }
     */
    @PutMapping("/api/products/{id}/decrease-stock")
    void decreaseStock(
        @PathVariable String id,
        @RequestParam Long quantity
    );
    
    /**
     * increaseStock - 상품 재고 증가 (보상 트랜잭션)
     * 
     * @param id 상품 ID
     * @param quantity 증가할 수량
     * 
     * 뜻풀이:
     * - decreaseStock과 동일한 방식으로 동작합니다
     * - 다만 /api/products/{id}/increase-stock으로 요청을 보냅니다
     * 
     * 실제 HTTP 요청:
     * PUT http://localhost:8081/api/products/PROD001/increase-stock?quantity=2
     * 
     * 호출 장면:
     * 주문 생성이 실패했을 때, OrderSagaOrchestrator의 compensate() 메서드에서
     * 이미 차감했던 재고를 복구하기 위해 호출됩니다.
     * 
     * 호출 예시:
     * productClient.increaseStock("PROD001", 2L);
     */
    @PutMapping("/api/products/{id}/increase-stock")
    void increaseStock(
        @PathVariable String id,
        @RequestParam Long quantity
    );
}
```

#### src/main/java/com/msa/order/entity/Order.java
```java
package com.msa.order.entity;

import com.msa.common.entity.BaseEntity;
import com.msa.common.entity.OrderStatus;
import lombok.*;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Order 엔티티 - 주문 정보를 나타냅니다
 * 
 * 데이터베이스 테이블: orders
 * 역할: 고객의 주문 정보를 저장합니다
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {
    
    /**
     * id - 주문 ID (UUID)
     * 
     * 뜻풀이:
     * - @Id: 이 필드가 PRIMARY KEY임을 나타냅니다
     * - @GeneratedValue(strategy = GenerationType.UUID)
     *   └─ UUID 전략으로 자동 생성합니다
     *   └─ 예: "550e8400-e29b-41d4-a716-446655440000"
     * - String 타입으로 UUID를 문자열로 저장합니다
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    /**
     * customerId - 고객 ID
     * 
     * 뜻풀이:
     * - @Column(nullable = false): NOT NULL 제약조건
     * - 어떤 고객이 주문했는지를 나타냅니다
     * - 예: "CUST001", "CUST002"
     */
    @Column(nullable = false)
    private String customerId;
    
    /**
     * items - 주문 항목들 (주문에 포함된 상품들)
     * 
     * 뜻풀이:
     * - @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
     *   ├─ One = Order (주문 1개)
     *   ├─ Many = OrderItem (상품 여러 개)
     *   ├─ mappedBy = "order": OrderItem의 order 필드로 관계가 정의됨
     *   ├─ cascade = CascadeType.ALL: Order가 삭제되면 관련 OrderItem도 자동으로 삭제
     *   └─ fetch = FetchType.EAGER: Order를 조회할 때 OrderItem도 함께 로드
     * 
     * - = new ArrayList<>(): 빈 리스트로 초기화
     * 
     * 예시:
     * Order order = new Order();
     * order.items = [
     *   OrderItem(productId="PROD001", quantity=2),
     *   OrderItem(productId="PROD002", quantity=1)
     * ]
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();
    
    /**
     * totalAmount - 주문 총액
     * 
     * 뜻풀이:
     * - @Column(nullable = false): NOT NULL 제약조건
     * - Long 타입으로 금액을 저장합니다 (정수 처리)
     * - 예: 30000 (30,000원)
     */
    @Column(nullable = false)
    private Long totalAmount;
    
    /**
     * status - 주문 상태
     * 
     * 뜻풀이:
     * - @Enumerated(EnumType.STRING)
     *   └─ Java의 enum 값을 문자열로 데이터베이스에 저장합니다
     *   └─ 예: "PENDING", "COMPLETED", "FAILED"
     * - @Column(nullable = false): NOT NULL 제약조건
     * - OrderStatus enum 값을 가집니다:
     *   ├─ PENDING: 주문 대기 중
     *   ├─ CONFIRMED: 주문 확인됨
     *   ├─ COMPLETED: 주문 완료
     *   ├─ CANCELLED: 주문 취소
     *   └─ FAILED: 주문 실패
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
}
```

#### src/main/java/com/msa/order/entity/OrderItem.java
```java
package com.msa.order.entity;

import com.msa.common.entity.BaseEntity;
import lombok.*;
import jakarta.persistence.*;

/**
 * OrderItem 엔티티 - 주문에 포함된 상품 항목을 나타냅니다
 * 
 * 데이터베이스 테이블: order_items
 * 역할: 각 주문에 포함된 개별 상품 정보를 저장합니다
 * 
 * 관계: Order ← OrderItem (One-to-Many)
 * 예: 1개의 Order가 여러 개의 OrderItem을 가질 수 있습니다
 */
@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {
    
    /**
     * id - 주문 항목 ID (UUID)
     * 
     * 뜻풀이:
     * - @Id: PRIMARY KEY
     * - @GeneratedValue(strategy = GenerationType.UUID): 자동 생성
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    /**
     * order - 이 항목이 속한 Order 객체 (역방향 관계)
     * 
     * 뜻풀이:
     * - @ManyToOne: Many OrderItem이 One Order에 속합니다
     * - @JoinColumn(name = "order_id"): 외래키(Foreign Key) 설정
     *   └─ 데이터베이스의 order_id 칼럼으로 Order와 연결됩니다
     * - fetch = FetchType.LAZY: OrderItem을 로드할 때 Order는 나중에 로드
     *   └─ 성능 최적화: 필요할 때만 Order 데이터를 조회합니다
     * 
     * 관계 설정:
     * Order.items (Order 쪽): mappedBy = "order"로 설정됨
     * OrderItem.order (OrderItem 쪽): @ManyToOne으로 설정됨
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    
    /**
     * productId - 상품 ID
     * 
     * 뜻풀이:
     * - @Column(nullable = false): NOT NULL 제약조건
     * - Product Service의 Product.id를 참조합니다
     * - 직접 외래키가 아님 (다른 서비스이므로 레퍼런스만 저장)
     * - 예: "PROD001", "PROD002"
     */
    @Column(nullable = false)
    private String productId;
    
    /**
     * quantity - 구매 수량
     * 
     * 뜻풀이:
     * - @Column(nullable = false): NOT NULL 제약조건
     * - Long 타입으로 수량을 저장합니다
     * - 예: 2 (2개 구매)
     */
    @Column(nullable = false)
    private Long quantity;
    
    /**
     * price - 상품 가격 (주문 시점의 가격)
     * 
     * 뜻풀이:
     * - @Column(nullable = false): NOT NULL 제약조건
     * - Long 타입으로 가격을 저장합니다
     * - 주문 시점의 가격을 저장하므로, 나중에 상품 가격이 바뀌어도 이 값은 변하지 않습니다
     * - 예: 15000 (15,000원)
     * 
     * 중요:
     * price = 15000, quantity = 2
     * → 이 상품의 소계 = 15000 × 2 = 30000원
     */
    @Column(nullable = false)
    private Long price;
}
```

#### src/main/java/com/msa/order/repository/OrderRepository.java
```java
package com.msa.order.repository;

import com.msa.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByCustomerId(String customerId);
}
```

#### src/main/java/com/msa/order/service/OrderService.java
```java
package com.msa.order.service;

import com.msa.common.dto.OrderDTO;
import com.msa.common.dto.OrderItemDTO;
import com.msa.common.entity.OrderStatus;
import com.msa.common.exception.ApiException;
import com.msa.order.entity.Order;
import com.msa.order.entity.OrderItem;
import com.msa.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator orderSagaOrchestrator;
    
    public OrderDTO getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException("주문을 찾을 수 없습니다", "ORDER_NOT_FOUND", 404));
        
        return toDTO(order);
    }
    
    public OrderDTO createOrder(OrderDTO dto) {
        return orderSagaOrchestrator.executeOrderSaga(dto);
    }
    
    public void updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException("주문을 찾을 수 없습니다", "ORDER_NOT_FOUND", 404));
        
        order.setStatus(status);
        orderRepository.save(order);
    }
    
    protected Order saveOrder(Order order) {
        return orderRepository.save(order);
    }
    
    protected OrderDTO toDTO(Order order) {
        return new OrderDTO(
            order.getId(),
            order.getCustomerId(),
            order.getItems().stream()
                .map(item -> new OrderItemDTO(item.getProductId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList()),
            order.getTotalAmount(),
            order.getStatus().name(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
```

#### src/main/java/com/msa/order/service/OrderSagaOrchestrator.java (Saga 패턴)
```java
package com.msa.order.service;

import com.msa.common.dto.OrderDTO;
import com.msa.common.dto.OrderItemDTO;
import com.msa.common.entity.OrderStatus;
import com.msa.common.exception.ApiException;
import com.msa.order.client.ProductClient;
import com.msa.order.entity.Order;
import com.msa.order.entity.OrderItem;
import com.msa.order.repository.OrderRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderSagaOrchestrator
 * 
 * Saga 패턴을 사용하여 분산 트랜잭션을 조율합니다.
 * 여러 마이크로서비스 간의 데이터 일관성을 보장합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {
    
    private final OrderRepository orderRepository;
    private final ProductClient productClient;  // OpenFeign 클라이언트
    
    /**
     * executeOrderSaga - 주문 생성을 위한 Saga 패턴 실행
     * 
     * 이 메서드는 @Transactional 어노테이션이 붙어있으므로,
     * 메서드 실행 중 RuntimeException이 발생하면 자동으로 롤백됩니다.
     * 
     * 흐름:
     * ┌─────────────────────────────────────┐
     * │  Step 1: Order 생성 (PENDING 상태)   │
     * └────────────────┬────────────────────┘
     *                  ↓
     * ┌─────────────────────────────────────┐
     * │  Step 2: Product Service 호출        │
     * │   - 각 상품의 재고 차감              │
     * └────────────────┬────────────────────┘
     *                  ↓
     *         ┌────────┴────────┐
     *    성공 ↓                  ↓ 실패
     * ┌──────────────┐    ┌───────────────────┐
     * │ Step 3       │    │ 보상 트랜잭션      │
     * │ 상태변경     │    │ (재고 복구)        │
     * │ COMPLETED    │    └───────────────────┘
     * └──────────────┘
     */
    @Transactional
    public OrderDTO executeOrderSaga(OrderDTO dto) {
        log.info("=== 주문 생성 Saga 시작 ===");
        log.info("고객 ID: {}, 주문액: {}", dto.getCustomerId(), dto.getTotalAmount());
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Step 1: Order 엔티티 생성 (PENDING 상태)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 설명:
        // - Order.builder()를 사용하여 Order 객체를 생성합니다 (빌더 패턴)
        // - customerId: 주문한 고객의 ID를 저장합니다
        // - totalAmount: 주문 총액을 저장합니다
        // - status: OrderStatus.PENDING으로 설정 (아직 확정되지 않은 상태)
        // - items: 빈 ArrayList로 초기화합니다 (아래에서 추가)
        Order order = Order.builder()
            .customerId(dto.getCustomerId())
            .totalAmount(dto.getTotalAmount())
            .status(OrderStatus.PENDING)
            .items(new ArrayList<>())
            .build();
        
        // 각 주문 항목(OrderItem)을 OrderItemDTO에서 변환하여 Order에 추가합니다
        // 설명:
        // - for 루프로 요청의 items 배열을 순회합니다
        // - 각 OrderItemDTO를 OrderItem 엔티티로 변환합니다
        // - productId: 상품 ID
        // - quantity: 구매 수량
        // - price: 상품 가격
        // - order: 양방향 관계 설정 (이 OrderItem이 속한 Order를 참조)
        // - order.getItems().add(item): Order의 items 리스트에 추가합니다
        for (OrderItemDTO itemDTO : dto.getItems()) {
            OrderItem item = OrderItem.builder()
                .productId(itemDTO.getProductId())
                .quantity(itemDTO.getQuantity())
                .price(itemDTO.getPrice())
                .order(order)  // 양방향 관계
                .build();
            order.getItems().add(item);
        }
        
        // orderRepository.save(order)로 Order를 데이터베이스에 저장합니다
        // 설명:
        // - INSERT 쿼리가 실행되어 order와 관련된 orderitem들이 DB에 저장됩니다
        // - 이 시점에는 재고가 아직 차감되지 않았습니다 (Product Service 호출 전)
        // - 만약 이 후 Product Service가 실패하면, 이 Order는 FAILED 상태로 변경됩니다
        order = orderRepository.save(order);
        log.info("Step 1 완료: Order 생성 (ID: {}, 상태: PENDING)", order.getId());
        
        try {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Step 2: Product Service 호출 - 재고 차감
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 설명:
            // - order.getItems()의 각 항목을 순회합니다
            // - productClient.decreaseStock()으로 Product Service를 호출합니다
            // - 이는 HTTP PUT 요청을 Product Service의 `/api/products/{id}/decrease-stock`로 보냅니다
            // - 만약 재고가 부족하면 FeignException.BadRequest 예외가 발생합니다
            for (OrderItem item : order.getItems()) {
                log.info("재고 차감 요청: 상품 ID={}, 수량={}", item.getProductId(), item.getQuantity());
                productClient.decreaseStock(item.getProductId(), item.getQuantity());
                log.info("재고 차감 성공: 상품 ID={}", item.getProductId());
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Step 3: Order 상태 변경 (COMPLETED)
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 설명:
            // - 모든 상품의 재고 차감에 성공했으므로, Order의 상태를 COMPLETED로 변경합니다
            // - COMPLETED = 주문이 확정되었음을 의미합니다
            // - 데이터베이스에 변경 사항을 저장합니다
            order.setStatus(OrderStatus.COMPLETED);
            order = orderRepository.save(order);
            log.info("Step 3 완료: Order 상태 변경 (COMPLETED)");
            log.info("=== 주문 생성 Saga 성공 ===");
            
            // Order 객체를 OrderDTO로 변환하여 반환합니다
            return toDTO(order);
            
        } catch (FeignException.BadRequest e) {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 예외 처리 1: 재고 부족 (BadRequest)
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            log.error("재고 부족: {}", e.getMessage());
            // compensate(order)를 호출하여 보상 트랜잭션을 실행합니다
            compensate(order);
            // ApiException을 던져 클라이언트에게 오류를 알립니다
            throw new ApiException("재고가 부족합니다", "INSUFFICIENT_STOCK", 400);
            
        } catch (FeignException.ServiceUnavailable e) {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 예외 처리 2: Product Service 사용 불가 (503)
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            log.error("Product Service 사용 불가: {}", e.getMessage());
            // Product Service가 다운되었을 때도 보상 트랜잭션을 실행합니다
            compensate(order);
            throw new ApiException("상품 서비스를 사용할 수 없습니다", "SERVICE_UNAVAILABLE", 503);
            
        } catch (Exception e) {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 예외 처리 3: 예상치 못한 모든 오류
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            log.error("예상치 못한 오류: {}", e.getMessage());
            compensate(order);
            throw new ApiException("주문 처리 중 오류 발생", "ORDER_FAILED", 500);
        }
    }
    
    /**
     * compensate - 보상 트랜잭션 (Compensating Transaction)
     * 
     * Saga 패턴의 핵심: 한 서비스의 작업이 실패하면,
     * 이전에 성공한 작업들을 모두 되돌립니다 (롤백 효과).
     * 
     * 예: Order 생성은 성공했지만 재고 차감이 실패
     *     → Order.status를 FAILED로 변경하고 (Order 부분 롤백)
     *     → 다른 서비스의 작업은 발생하지 않았으므로 (Product 재고 변경 안 됨)
     *     → 전체적으로 데이터 일관성 유지
     */
    private void compensate(Order order) {
        log.warn("=== 보상 트랜잭션 시작 (Order ID: {}) ===", order.getId());
        
        try {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Part 1: Product Service에 재고 복구 요청
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 설명:
            // - 주문 항목 중 이미 차감된 재고가 있을 수 있으므로,
            //   현재 Order에 속한 모든 OrderItem을 순회합니다
            // - productClient.increaseStock()으로 재고를 복구합니다
            // - 예: 노트북 2개를 차감했다면, 2개를 다시 추가하여 복구합니다
            // - 이 호출이 성공해야만 데이터 일관성이 유지됩니다
            for (OrderItem item : order.getItems()) {
                log.info("재고 복구 요청: 상품 ID={}, 수량={}", item.getProductId(), item.getQuantity());
                productClient.increaseStock(item.getProductId(), item.getQuantity());
                log.info("재고 복구 완료: 상품 ID={}", item.getProductId());
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Part 2: Order 상태를 FAILED로 변경
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 설명:
            // - Order는 이미 PENDING 상태로 저장되었으므로,
            //   FAILED 상태로 변경하여 "이 주문은 완료되지 않았습니다"를 표시합니다
            // - PENDING → FAILED 상태 전환으로 주문 실패를 기록합니다
            // - 나중에 관리자가 조회할 때 "실패한 주문" 목록에서 찾을 수 있습니다
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            log.warn("Order 상태 변경 (FAILED)");
            log.warn("=== 보상 트랜잭션 완료 ===");
            
        } catch (Exception e) {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Part 3: 보상 트랜잭션 자체가 실패한 경우
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 설명:
            // 예를 들어, 재고를 복구하려 했는데 Product Service가 다운되어 있으면?
            // 또는 네트워크가 끊어져서 요청을 보낼 수 없으면?
            // 
            // 이 경우, 강제로 Order를 FAILED 상태로 저장합니다.
            // 그리고 로그를 남겨서 관리자가 수동으로 개입할 수 있도록 합니다.
            // 
            // 실전 예시:
            // 1. Order A를 생성 (PENDING)
            // 2. 상품 재고를 2개 차감 시도 → 실패 (Product Service 다운)
            // 3. 보상 트랜잭션: 재고 복구 시도 → 실패 (여전히 다운 상태)
            // 4. Order A를 FAILED로 표시하고 로그 남김
            // 5. 관리자가 "Order A는 차감에 실패했지만, 재고 복구도 실패했다"는 것을 알게 됩니다
            log.error("보상 트랜잭션 중 오류 발생: {}", e.getMessage());
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            log.error("Order를 FAILED 상태로 업데이트했습니다. 관리자 개입 필요 - Order ID: {}", order.getId());
        }
    }
    
    /**
     * toDTO - Order 엔티티를 OrderDTO로 변환
     * 
     * 설명:
     * - Order 엔티티는 JPA 영속성 객체입니다 (DB와 매핑)
     * - OrderDTO는 API 응답용 DTO입니다 (클라이언트에게 반환)
     * - Stream API의 map()을 사용하여 OrderItem → OrderItemDTO로 변환합니다
     */
    private OrderDTO toDTO(Order order) {
        return new OrderDTO(
            order.getId(),
            order.getCustomerId(),
            order.getItems().stream()
                .map(item -> new OrderItemDTO(item.getProductId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList()),
            order.getTotalAmount(),
            order.getStatus().name(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
```

#### src/main/java/com/msa/order/controller/OrderController.java
```java
package com.msa.order.controller;

import com.msa.common.dto.OrderDTO;
import com.msa.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OrderController
 * 
 * 주문 서비스의 REST API를 제공합니다.
 * 클라이언트는 이 컨트롤러의 엔드포인트를 호출하여 주문을 생성하고 조회합니다.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * GET /api/orders/{id}
     * 
     * 특정 주문 정보 조회
     * 
     * @param id 주문 ID
     * @return 주문 정보 (OrderDTO)
     * 
     * 뜻풀이:
     * - @GetMapping("/{id}")는 HTTP GET 요청을 처리합니다
     * - @PathVariable String id는 URL 경로의 {id} 값을 메서드 파라미터로 받습니다
     * - 예: GET /api/orders/ORD001 → id = "ORD001"
     * - ResponseEntity.ok()로 200 OK 상태코드와 함께 주문 정보를 반환합니다
     * 
     * 응답 예시:
     * {
     *   "id": "ORD001",
     *   "customerId": "CUST001",
     *   "items": [...],
     *   "totalAmount": 30000,
     *   "status": "COMPLETED",
     *   "createdAt": "2025-11-22T10:30:00",
     *   "updatedAt": "2025-11-22T10:30:05"
     * }
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }
    
    /**
     * POST /api/orders
     * 
     * 새로운 주문 생성 (Saga 패턴으로 처리)
     * 
     * @param dto 주문 정보 (OrderDTO)
     * @return 생성된 주문 정보
     * 
     * 뜻풀이:
     * - @PostMapping은 HTTP POST 요청을 처리합니다
     * - @RequestBody OrderDTO는 JSON 요청 본문을 OrderDTO 객체로 변환합니다
     * - ResponseEntity.status(HttpStatus.CREATED)로 201 Created 상태코드를 반환합니다
     * - body()로 생성된 주문 정보를 응답 본문에 포함합니다
     * 
     * 요청 예시:
     * POST /api/orders
     * Content-Type: application/json
     * 
     * {
     *   "customerId": "CUST001",
     *   "items": [
     *     {
     *       "productId": "PROD001",
     *       "quantity": 2,
     *       "price": 15000
     *     }
     *   ],
     *   "totalAmount": 30000
     * }
     * 
     * 내부 처리 흐름 (Saga 패턴):
     * 1. OrderController.createOrder() 호출
     * 2. OrderService.createOrder() 호출
     * 3. OrderSagaOrchestrator.executeOrderSaga() 호출 (핵심 로직)
     *    ├─ Step 1: Order 생성 (PENDING)
     *    ├─ Step 2: Product Service 호출 (재고 차감)
     *    ├─ Step 3: Order 상태 변경 (COMPLETED)
     *    └─ 실패 시: 보상 트랜잭션 실행
     * 4. OrderDTO로 변환하여 클라이언트에 반환
     * 
     * ✅ 성공 응답 (201 Created):
     * {
     *   "id": "ORD123",
     *   "customerId": "CUST001",
     *   "items": [
     *     {
     *       "productId": "PROD001",
     *       "quantity": 2,
     *       "price": 15000
     *     }
     *   ],
     *   "totalAmount": 30000,
     *   "status": "COMPLETED",
     *   "createdAt": "2025-11-22T10:30:00",
     *   "updatedAt": "2025-11-22T10:30:05"
     * }
     * 
     * ❌ 실패 응답 (400/500):
     * {
     *   "code": "INSUFFICIENT_STOCK",
     *   "message": "재고가 부족합니다",
     *   "status": 400
     * }
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO dto) {
        // orderService.createOrder()를 호출합니다
        // 이 메서드는 OrderSagaOrchestrator.executeOrderSaga()를 호출하여
        // Saga 패턴을 실행합니다
        OrderDTO createdOrder = orderService.createOrder(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createdOrder);
    }
}
```

#### src/main/java/com/msa/order/OrderServiceApplication.java
```java
package com.msa.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.msa.common", "com.msa.order"})
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

---

## build.gradle.kts 설정

### Root build.gradle.kts
```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.5.7" apply false
    id("io.spring.dependency-management") version "1.1.6"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

allprojects {
    group = "com.msa"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    
    repositories {
        mavenCentral()
    }
    
    dependencies {
        // 공통 의존성
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        
        // 테스트
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
}
```

### common/build.gradle.kts
```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.mysql:mysql-connector-j:8.0.33")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
```

### product/build.gradle.kts
```kotlin
dependencies {
    implementation(project(":common"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.mysql:mysql-connector-j:8.0.33")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
```

### order/build.gradle.kts
```kotlin
dependencies {
    implementation(project(":common"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.3")
    implementation("com.mysql:mysql-connector-j:8.0.33")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
    }
}
```

### settings.gradle.kts
```kotlin
rootProject.name = "commerce"
include(":common")
include(":product")
include(":order")
```

---

## 실행 순서

### 1️⃣ 터미널에서 순서대로 실행

```bash
# Product Service 시작 (Port: 8081)
cd commerce
./gradlew :product:bootRun

# 다른 터미널에서 Order Service 시작 (Port: 8082)
./gradlew :order:bootRun
```

### 2️⃣ API 테스트

#### **1단계: Product Service에 상품 생성**

```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "노트북",
    "price": 1500000,
    "quantity": 10,
    "description": "고성능 노트북"
  }'
```

**뜻풀이:**
- `curl -X POST`: HTTP POST 요청을 보냅니다
- `http://localhost:8081/api/products`: Product Service의 상품 생성 엔드포인트
- `-H "Content-Type: application/json"`: JSON 형식으로 요청을 보냅니다
- `-d '...'`: 요청 본문 (상품 정보)
  - `name`: "노트북" (상품 이름)
  - `price`: 1500000 (상품 가격)
  - `quantity`: 10 (초기 재고)
  - `description`: "고성능 노트북" (설명)

**응답 (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "노트북",
  "price": 1500000,
  "quantity": 10,
  "description": "고성능 노트북"
}
```

**응답 설명:**
- `id`: 데이터베이스에 저장된 상품 ID (자동 생성된 UUID)
- 이 ID를 다음 단계(주문 생성)에서 사용합니다

---

#### **2단계: Order Service에서 주문 생성** (Saga 패턴 실행)

**성공 시나리오:**
```bash
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "items": [
      {
        "productId": "550e8400-e29b-41d4-a716-446655440000",
        "quantity": 2,
        "price": 1500000
      }
    ],
    "totalAmount": 3000000
  }'
```

**뜻풀이:**
- `http://localhost:8082/api/orders`: Order Service의 주문 생성 엔드포인트
- `customerId`: "CUST001" (고객 ID)
- `items`: 주문에 포함될 상품들
  - `productId`: 위에서 생성한 상품 ID
  - `quantity`: 2 (2개 구매)
  - `price`: 1500000 (1개 가격)
- `totalAmount`: 3000000 (총 금액 = 1500000 × 2)

**내부 처리 흐름 (Saga 패턴):**
```
1. Order 생성 (PENDING)
   └─ INSERT INTO orders (customerId, totalAmount, status)
   └─ INSERT INTO order_items (productId, quantity, price)
   
2. Product Service 호출
   └─ PUT /api/products/550e8400-e29b-41d4-a716-446655440000/decrease-stock?quantity=2
   └─ Product의 quantity: 10 - 2 = 8
   
3. Order 상태 변경 (PENDING → COMPLETED)
   └─ UPDATE orders SET status = 'COMPLETED'
```

**성공 응답 (201 Created):**
```json
{
  "id": "ORD123",
  "customerId": "CUST001",
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "quantity": 2,
      "price": 1500000
    }
  ],
  "totalAmount": 3000000,
  "status": "COMPLETED",
  "createdAt": "2025-11-22T10:30:00",
  "updatedAt": "2025-11-22T10:30:05"
}
```

**응답 설명:**
- `id`: 생성된 주문 ID
- `status`: "COMPLETED" = 주문이 성공적으로 완료됨
- `createdAt`, `updatedAt`: 생성 시간과 수정 시간

---

**실패 시나리오 (재고 부족):**

만약 quantity를 20으로 요청하면:
```bash
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "items": [
      {
        "productId": "550e8400-e29b-41d4-a716-446655440000",
        "quantity": 20,  # ← 재고 부족 (재고는 10개)
        "price": 1500000
      }
    ],
    "totalAmount": 30000000
  }'
```

**실패 응답 (400 Bad Request):**
```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "재고가 부족합니다",
  "status": 400
}
```

**내부 처리 흐름 (보상 트랜잭션):**
```
1. Order 생성 (PENDING) ✅ 성공
   
2. Product Service 호출 ❌ 실패
   └─ PUT /api/products/.../decrease-stock?quantity=20
   └─ 재고 부족 예외 발생
   
3. 보상 트랜잭션 실행
   └─ 재고 복구: PUT /api/products/.../increase-stock?quantity=0
   └─ Order 상태 변경: FAILED
   
결과: 
- Order: FAILED 상태로 저장됨
- Product: 변경 없음 (재고는 여전히 10개)
- 클라이언트: 400 에러 응답
```

---

#### **3단계: 주문 조회**

```bash
# 성공한 주문 조회
curl http://localhost:8082/api/orders/ORD123
```

**뜻풀이:**
- GET /api/orders/{orderId}로 주문 정보를 조회합니다
- ORD123: 위에서 생성된 주문 ID

**응답:**
```json
{
  "id": "ORD123",
  "customerId": "CUST001",
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "quantity": 2,
      "price": 1500000
    }
  ],
  "totalAmount": 3000000,
  "status": "COMPLETED",
  "createdAt": "2025-11-22T10:30:00",
  "updatedAt": "2025-11-22T10:30:05"
}
```

---

## 흐름도

```
┌─────────────────────────────────────────────────────────────┐
│                    클라이언트 요청                            │
│         POST /api/orders (주문 생성)                        │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
    ┌──────────────────────┐
    │  Order Service       │
    │  OrderController     │
    └──────────┬───────────┘
               │
               ▼
    ┌──────────────────────────────┐
    │  OrderSagaOrchestrator       │
    │  Saga 패턴으로 분산 TX 조율   │
    └──────────┬──────────────────┘
               │
        ┌──────┴──────┐
        │ Step 1      │ Step 2
        ▼             ▼
    Order 생성   ProductClient
    (PENDING)    (OpenFeign)
               │
               ▼
    ┌──────────────────────┐
    │  Product Service     │
    │  decreaseStock()     │
    │  재고 차감           │
    └──────────┬───────────┘
               │
        ┌──────┴──────┐
        │ Success     │ Failure (재고 부족)
        ▼             ▼
    Step 3     compensate()
    COMPLETED  ├─ 재고 복구
               ├─ Order.status=FAILED
               └─ 예외 발생
```

---

## 특징

✅ **멀티모듈 구조**: common 모듈로 공통 코드 관리  
✅ **Saga 패턴**: OrderSagaOrchestrator에서 분산 트랜잭션 관리  
✅ **OpenFeign**: ProductClient로 서비스 간 통신  
✅ **보상 트랜잭션**: 실패 시 자동으로 재고 복구  
✅ **로깅**: 각 단계별 상세 로그 출력  
✅ **예외 처리**: 커스텀 ApiException으로 통일  

---

## 다음 단계

1. **Spring Cloud Config Server**: 설정 외부화
2. **Eureka Server**: 서비스 디스커버리 (하드코딩된 URL 제거)
3. **API Gateway**: 단일 진입점
4. **Resilience4j**: Circuit Breaker 추가
5. **Kafka**: 비동기 이벤트 처리
6. **Sleuth + Zipkin**: 분산 추적
