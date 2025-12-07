rootProject.name = "msa"

include("config", "eureka", "license", "company", "gateway")

project(":config").projectDir = file("service/config")
project(":eureka").projectDir = file("service/eureka")
project(":license").projectDir = file("service/license")
project(":company").projectDir = file("service/company")
project(":gateway").projectDir = file("service/gateway")