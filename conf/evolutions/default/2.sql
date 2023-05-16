# -- !Ups

create table stripe_user_payment_methods(
    user_id uuid not null constraint pk_stripe_user_payment_methods primary key,
    customer_id varchar not null,
    payment_method_id varchar not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

# -- !Downs