create table users (
    id uuid not null,
    email varchar(255) not null,
    password_hash varchar(255) not null,
    role varchar(255) not null,
    created_at timestamp(6) with time zone not null,
    constraint users_pkey primary key (id),
    constraint uk_users_email unique (email),
    constraint users_role_check check (role in ('ADMIN', 'USER'))
);
