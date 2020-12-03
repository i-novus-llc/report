DROP TABLE IF EXISTS EMPLOYEE;

CREATE TABLE EMPLOYEE
(
    FULL_NAME VARCHAR,
    AGE       INT,
    SALARY    INT
);

INSERT INTO EMPLOYEE (FULL_NAME, AGE, SALARY)
VALUES ('Misha', 25, 1000),
       ('Sasha', 30, 1500),
       ('Kolya', 36, 2000);