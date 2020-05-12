-- sqlite3

.separator "\t"
.import mrFrumbleData.txt Frumble

select * from Frumble as F1, Frumble as F2 
where F1.name=F2.name and F1.price!=F2.price
limit 10;-- 0: name -> price

select * from Frumble as F1, Frumble as F2 
where F1.name=F2.name and F1.discount != F2.discount 
limit 10;-- do not find

select * from Frumble as F1, Frumble as F2 
where F1.month=F2.month and F1.discount != F2.discount 
limit 10;-- 0: month -> discount

create table t1(name TEXT, month TEXT, primary key(name, month));
create table t2(t1_name TEXT, price TEXT, foreign key(t1_name) references t1(name));
create table t3(t1_month TEXT, discount TEXT, foreign key(t1_month) references t1(month));

replace into t1 select name, month from Frumble group by name, month;
replace into t2 select name, price from Frumble group by name;
replace into t3 select month, discount from Frumble group by month;
select count(*) from t1;-- 426
select count(*) from t2;-- 36
select count(*) from t3;-- 12