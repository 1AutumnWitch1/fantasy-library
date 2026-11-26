-- Switch the backend to its previous version first. Export these tables to keep any newly collected viewing data.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
DROP TABLE fl_play_session;
DROP TABLE fl_episode_progress;
DROP TABLE fl_play_total;
