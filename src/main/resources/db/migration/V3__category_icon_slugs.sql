ALTER TABLE categories RENAME COLUMN image TO icon;
UPDATE categories SET icon = REPLACE(icon, 'R.drawable.', '');
UPDATE categories SET icon = 'none' WHERE icon = 'sem_foto';