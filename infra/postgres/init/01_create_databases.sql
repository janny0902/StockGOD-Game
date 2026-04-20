SELECT 'CREATE DATABASE cldc_batch'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cldc_batch')\gexec

SELECT 'CREATE DATABASE cldc_ai_collector'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cldc_ai_collector')\gexec
