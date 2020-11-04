-- create tables
create table process
(
  bestbez           number(8),
  linr              number(6),
  kurzn             varchar2(10 char)
  );
--  

create table process_pos
(
  bestbez            number(8),
  posbez             number(6),
  artikel            varchar2(18 char),
  menge              number,
  ekpr               number  
  );
--

-- fill with records 
begin
    insert into process (bestbez, linr, kurzn) values (90310064, 70010, 'LieferantB');
    insert into process (bestbez, linr, kurzn) values (310035, 70007, 'LieferantA');
    insert into process (bestbez, linr, kurzn) values (90310068, 70007, 'LieferantA');
    insert into process (bestbez, linr, kurzn) values (310038, 70007, 'LieferantA');
    insert into process (bestbez, linr, kurzn) values (90310084, 70010, 'LieferantB');
    insert into process (bestbez, linr, kurzn) values (90310069, 70010, 'LieferantB');
    insert into process (bestbez, linr, kurzn) values (90310075, 70007, 'LieferantA');
    insert into process (bestbez, linr, kurzn) values (90310078, 70010, 'LieferantB');
    --
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (90310064, 10, 'ArtikelB', 15, 138.60);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values ( 310035, 10, 'KaufteilA', 25, 124.55);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (90310068, 10, 'ArtikelA', 40, 216.20);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (90310068, 20, 'BaugruppeA', 50, 86.50);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (90310068, 30, 'MusterA', 2, 52.00);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (310038, 10, 'ArtikelA', 12, 216.20);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (310038, 20, 'MusterA', 5, 52.00);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (90310084, 10, 'MusterB', 3, 52.00);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (90310069, 10, 'KaufteilB', 30, 124.55);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (90310075, 10, 'ArtikelA', 18, 216.20);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (90310075, 20, 'BaugruppeA', 80, 86.50);
    insert into process_pos (bestbez, posbez, artikel, menge, ekpr) values (90310078, 10, 'ArtikelB', 35, 138.60);
    --
    commit;  
end;

-- create report using view
create or replace view r_process
as
select p.bestbez, p.linr, p.kurzn,
        po.posbez, po.artikel, po. menge, po.ekpr  
from process_pos po
        join process p
              on (po.bestbez = p.bestbez)
