insert into tb_billing_plan (name, monthly_fee, active)
select 'Básico', 199.90, true
where not exists (
    select 1 from tb_billing_plan where lower(name) = lower('Básico')
);

insert into tb_billing_plan (name, monthly_fee, active)
select 'Profissional', 349.90, true
where not exists (
    select 1 from tb_billing_plan where lower(name) = lower('Profissional')
);

insert into tb_billing_plan (name, monthly_fee, active)
select 'Premium', 499.90, true
where not exists (
    select 1 from tb_billing_plan where lower(name) = lower('Premium')
);

insert into tb_billing_plan_feature (billing_plan_id, feature_id)
select bp.id, f.id
from tb_billing_plan bp
cross join tb_feature f
where lower(bp.name) in (lower('Básico'), lower('Profissional'), lower('Premium'))
  and not exists (
      select 1
      from tb_billing_plan_feature bpf
      where bpf.billing_plan_id = bp.id
        and bpf.feature_id = f.id
  );
