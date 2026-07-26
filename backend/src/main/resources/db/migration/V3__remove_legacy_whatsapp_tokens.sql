UPDATE public.social_channels
SET access_token = '',
    refresh_token = NULL
WHERE platform = 'WHATSAPP'
  AND (access_token <> '' OR refresh_token IS NOT NULL);
