-- Migration: ajout des colonnes PIN sur la table bank_account
-- À exécuter manuellement si ddl-auto != update, ou conserver comme référence.

ALTER TABLE bank_account
    ADD COLUMN IF NOT EXISTS pin_hash                    VARCHAR(255)    NULL,
    ADD COLUMN IF NOT EXISTS pin_attempt_count           INT             NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_pin_attempt            DATETIME        NULL,
    ADD COLUMN IF NOT EXISTS pin_counter_under_threshold INT             NOT NULL DEFAULT 0;
