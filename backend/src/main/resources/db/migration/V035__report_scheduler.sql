-- V035: Report Scheduler for automated delivery via Email and WhatsApp
-- Enables scheduled business reports (Sales, Payments, Profit & Loss, Low Stock)
-- delivered on Daily, Weekly, or Monthly schedules via branded HTML Email and WhatsApp summaries.

CREATE TABLE IF NOT EXISTS report_schedules (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    branch_id VARCHAR(36) REFERENCES branches(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    time_of_day VARCHAR(5) NOT NULL DEFAULT '20:00',
    day_of_week INT,
    day_of_month INT,
    channels VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    email_recipients TEXT,
    whatsapp_recipients TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMP WITH TIME ZONE,
    last_status VARCHAR(20),
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_report_schedule_type CHECK (report_type IN ('SALES_SUMMARY', 'PAYMENTS', 'PROFIT_LOSS', 'LOW_STOCK')),
    CONSTRAINT chk_report_schedule_freq CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_report_schedule_channels CHECK (channels IN ('EMAIL', 'WHATSAPP', 'EMAIL,WHATSAPP'))
);

CREATE INDEX IF NOT EXISTS idx_report_schedules_business ON report_schedules(business_id);
CREATE INDEX IF NOT EXISTS idx_report_schedules_active ON report_schedules(is_active);

CREATE TABLE IF NOT EXISTS report_schedule_logs (
    id VARCHAR(36) PRIMARY KEY,
    schedule_id VARCHAR(36) NOT NULL REFERENCES report_schedules(id) ON DELETE CASCADE,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    report_type VARCHAR(50) NOT NULL,
    period VARCHAR(100) NOT NULL,
    channels VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    summary_text TEXT,
    recipients_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_report_schedule_logs_schedule ON report_schedule_logs(schedule_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_report_schedule_logs_business ON report_schedule_logs(business_id, created_at DESC);
