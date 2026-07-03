-- Orders placed before authentication was introduced have no owner (user_id stays null).
alter table orders add column user_id uuid;
alter table orders add constraint fk_orders_user foreign key (user_id) references users (id);
