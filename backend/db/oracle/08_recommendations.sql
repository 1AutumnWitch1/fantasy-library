-- Apply after 07_account_avatar.sql. Existing works have no recorded intake date.
ALTER TABLE fl_work ADD (added_at TIMESTAMP WITH TIME ZONE);
ALTER TABLE fl_work MODIFY (added_at DEFAULT SYSTIMESTAMP);
ALTER TABLE fl_work ADD (editorial_rank NUMBER(6) CHECK (editorial_rank > 0));

UPDATE fl_work
SET editorial_rank = CASE slug
    WHEN 'anime-mist-harbor' THEN 10
    WHEN 'game-ash-vigil' THEN 20
    WHEN 'anime-star-court' THEN 30
    WHEN 'game-moss-greenhouse' THEN 40
END
WHERE slug IN ('anime-mist-harbor', 'game-ash-vigil', 'anime-star-court', 'game-moss-greenhouse');

COMMIT;
