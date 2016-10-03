-- liquibase formatted sql
-- changeset tim:201608191540 failOnError: true

create table payment_details (
    id int4 not null, amount int4,
    application_reference varchar(255),
    description varchar(255),
    payment_id varchar(255),
    payment_reference varchar(255),
    response TEXT, return_url text,
    service_id varchar(255),
    primary key (id)
);

alter table payment.payment_details owner to payment;