-- Seed/Update production CyberSource credentials for all existing businesses
INSERT INTO cybersource_configs (
    business_id,
    merchant_id,
    merchant_key_id,
    merchant_secret_key,
    profile_id,
    access_key,
    environment,
    created_at,
    updated_at
)
SELECT
    id AS business_id,
    'roakswahiliwearltd_t2p_ke',
    'f82a7e397ee238c99debbfe04efceb15',
    '51e8916680ec49b3afab8a1c60b39e5a81bcd216bdc349f7bd5be2dbb6169688028aa14facd44344aaa6c14d33234e530ea495aedc8249f9b4435569dc3603b0c3a10df3a5084b85b922d509fe4e08f8179dbe7978974f6fbab95c0048d807cd7b10eed553064f2aa3400a4e985e9ab4db39f6089004491fba2d578da9b7cab2',
    '632ECA4B-6F81-44A2-A6CD-C0C362D55F85',
    'f82a7e397ee238c99debbfe04efceb15',
    'production',
    NOW(),
    NOW()
FROM businesses
ON CONFLICT (business_id) DO UPDATE SET
    merchant_id         = EXCLUDED.merchant_id,
    merchant_key_id     = EXCLUDED.merchant_key_id,
    merchant_secret_key = EXCLUDED.merchant_secret_key,
    profile_id          = EXCLUDED.profile_id,
    access_key          = EXCLUDED.access_key,
    environment         = EXCLUDED.environment,
    updated_at          = NOW();
