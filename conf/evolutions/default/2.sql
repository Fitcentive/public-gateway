# -- !Ups

create table user_trial_status(
    user_id uuid not null constraint pk_user_trial_status primary key,
    has_been_used boolean not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

# -- !Downs