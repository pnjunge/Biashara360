UPDATE businesses
SET enabled_menus = CASE
    WHEN enabled_menus = '' THEN 'OPEN_TABS'
    WHEN POSITION('OPEN_TABS' IN enabled_menus) = 0 THEN enabled_menus || ',OPEN_TABS'
    ELSE enabled_menus
END;
