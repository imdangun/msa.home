set serveroutput on

declare
  type company_names is table of varchar2(100);
  companies company_names := company_names(
    'LG CNS', '삼성 SDS', 'SK하이닉스', '현대자동차', '네이버',
    '카카오', '쿠팡', '배달의민족', '토스', '라인');
  license_cnt number;
  license_id number;
begin
  delete from company_license;
  delete from companies;

  for i in 1..companies.count loop
    insert into companies(company_id, company_name, created_date)
    values (i, companies(i), sysdate-(110-i*10));
  end loop;

  dbms_output.put_line('회사 ' || companies.count || '개 생성했습니다.');

  for i in 1..10 loop
    license_cnt := trunc(dbms_random.value(1, 5));

    for j in 1..license_cnt loop
      begin
        license_id := trunc(dbms_random.value(1, 11));
        insert into company_license(company_id, license_id)
        values (i, license_id);
      exception
        when dup_val_on_index then
          null;
      end;
    end loop;

    dbms_output.put_line(companies(i) || ': 라이선스 ' || license_cnt || '개 할당했습니다.');
  end loop;

  commit;
  dbms_output.put_line('데이터 생성을 완료했습니다.');
end;
/