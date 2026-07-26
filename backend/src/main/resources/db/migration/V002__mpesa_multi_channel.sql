DO $migration$
DECLARE
    constraint_name text;
BEGIN
    IF to_regclass('public.mpesa_configs') IS NULL THEN
        RETURN;
    END IF;

    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        WHERE ns.nspname = current_schema()
          AND rel.relname = 'mpesa_configs'
          AND con.contype = 'u'
          AND (
              SELECT array_agg(CAST(att.attname AS text) ORDER BY key_columns.ordinality)
              FROM unnest(con.conkey) WITH ORDINALITY AS key_columns(attnum, ordinality)
              JOIN pg_attribute att
                ON att.attrelid = rel.oid
               AND att.attnum = key_columns.attnum
          ) = ARRAY['business_id']
    LOOP
        EXECUTE format('ALTER TABLE mpesa_configs DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.mpesa_configs'::regclass
          AND conname = 'mpesa_configs_business_id_account_type_unique'
    ) THEN
        ALTER TABLE public.mpesa_configs
            ADD CONSTRAINT mpesa_configs_business_id_account_type_unique
            UNIQUE (business_id, account_type);
    END IF;
END
$migration$;
