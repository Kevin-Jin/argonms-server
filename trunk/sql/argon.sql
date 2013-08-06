/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

 DROP TABLE IF EXISTS `infractions`;
  DROP TABLE IF EXISTS `macbans`;
 DROP TABLE IF EXISTS `bans`;
  DROP TABLE IF EXISTS `wishlists`;
  DROP TABLE IF EXISTS `skillmacros`;
  DROP TABLE IF EXISTS `skills`;
  DROP TABLE IF EXISTS `parties`;
  DROP TABLE IF EXISTS `minigamescores`;
  DROP TABLE IF EXISTS `mapmemory`;
  DROP TABLE IF EXISTS `keymaps`;
    DROP TABLE IF EXISTS `guildmembers`;
      DROP TABLE IF EXISTS `guildbbsreplies`;
    DROP TABLE IF EXISTS `guildbbstopics`;
  DROP TABLE IF EXISTS `guilds`;
  DROP TABLE IF EXISTS `famelog`;
  DROP TABLE IF EXISTS `cooldowns`;
  DROP TABLE IF EXISTS `buddyentries`;
   DROP TABLE IF EXISTS `questmobprogress`;
  DROP TABLE IF EXISTS `queststatuses`;
   DROP TABLE IF EXISTS `cashshoppurchases`;
   DROP TABLE IF EXISTS `inventoryrings`;
   DROP TABLE IF EXISTS `inventorypets`;
   DROP TABLE IF EXISTS `inventorymounts`;
   DROP TABLE IF EXISTS `inventoryequipment`;
  DROP TABLE IF EXISTS `inventoryitems`;
 DROP TABLE IF EXISTS `characters`;
DROP TABLE IF EXISTS `accounts`;

CREATE TABLE `accounts` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(12) NOT NULL,
  `password` VARBINARY(64) NOT NULL,
  `salt` BINARY(16),
  `pin` CHAR(4),
  `gender` TINYINT(2) NOT NULL DEFAULT 10,
  `birthday` INT(8),
  `characters` TINYINT(1) NOT NULL DEFAULT 3,
  `connected` TINYINT(1) NOT NULL DEFAULT 0,
  `recentmacs` VARBINARY(252),
  `recentip` INT(10) UNSIGNED,
  `storageslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 4,
  `storagemesos` INT(11) NOT NULL DEFAULT 0,
  `gm` TINYINT(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY (`name`)
) ENGINE = InnoDB;

CREATE TABLE `characters` (
  `accountid` INT(11) NOT NULL,
  `world` TINYINT(2) NOT NULL,
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(12) NOT NULL,
  `gender` TINYINT(1) NOT NULL,
  `skin` TINYINT(4) NOT NULL,
  `eyes` SMALLINT(6) NOT NULL,
  `hair` SMALLINT(6) NOT NULL,
  `level` TINYINT(3) UNSIGNED NOT NULL DEFAULT 1,
  `job` SMALLINT(6) NOT NULL DEFAULT 0,
  `str` SMALLINT(6) NOT NULL,
  `dex` SMALLINT(6) NOT NULL,
  `int` SMALLINT(6) NOT NULL,
  `luk` SMALLINT(6) NOT NULL,
  `hp` SMALLINT(6) NOT NULL DEFAULT 50,
  `maxhp` SMALLINT(6) NOT NULL DEFAULT 50,
  `mp` SMALLINT(6) NOT NULL DEFAULT 50,
  `maxmp` SMALLINT(6) NOT NULL DEFAULT 50,
  `ap` SMALLINT(6) NOT NULL DEFAULT 0,
  `sp` SMALLINT(6) NOT NULL DEFAULT 0,
  `exp` INT(11) NOT NULL DEFAULT 0,
  `fame` SMALLINT(6) NOT NULL DEFAULT 0,
  `spouse` INT(11) NOT NULL DEFAULT 0,
  `map` INT(11) NOT NULL DEFAULT 0,
  `spawnpoint` TINYINT(4) NOT NULL DEFAULT 0,
  `mesos` INT(11) NOT NULL DEFAULT 0,
  `equipslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `useslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `setupslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `etcslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `cashslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `buddyslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 20,
  `gm` TINYINT(4) NOT NULL,
  `overallrankcurrentpos` INT(11) NOT NULL DEFAULT 0,
  `overallrankoldpos` INT(11) NOT NULL DEFAULT 0,
  `worldrankcurrentpos` INT(11) NOT NULL DEFAULT 0,
  `worldrankoldpos` INT(11) NOT NULL DEFAULT 0,
  `jobrankcurrentpos` INT(11) NOT NULL DEFAULT 0,
  `jobrankoldpos` INT(11) NOT NULL DEFAULT 0,
  `famerankcurrentpos` INT(11) NOT NULL DEFAULT 0,
  `famerankoldpos` INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY (`name`),
  KEY (`accountid`),
  CONSTRAINT FOREIGN KEY (`accountid`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `inventoryitems` (
  `inventoryitemid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11),
  `accountid` INT(11),
  `inventorytype` TINYINT(1),
  `position` SMALLINT(6) NOT NULL,
  `itemid` INT(11) NOT NULL,
  `expiredate` BIGINT(20) NOT NULL,
  `owner` VARCHAR(12),
  `quantity` SMALLINT(6) NOT NULL,
  PRIMARY KEY (`inventoryitemid`),
  KEY (`characterid`),
  KEY (`accountid`),
  KEY (`characterid`,`inventorytype`),
  KEY (`accountid`,`inventorytype`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE,
  CONSTRAINT FOREIGN KEY (`accountid`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `inventoryequipment` (
  `inventoryequipmentid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` INT(10) UNSIGNED NOT NULL,
  `upgradeslots` TINYINT(4) NOT NULL,
  `level` TINYINT(4) NOT NULL,
  `str` SMALLINT(6) NOT NULL,
  `dex` SMALLINT(6) NOT NULL,
  `int` SMALLINT(6) NOT NULL,
  `luk` SMALLINT(6) NOT NULL,
  `hp` SMALLINT(6) NOT NULL,
  `mp` SMALLINT(6) NOT NULL,
  `watk` SMALLINT(6) NOT NULL,
  `matk` SMALLINT(6) NOT NULL,
  `wdef` SMALLINT(6) NOT NULL,
  `mdef` SMALLINT(6) NOT NULL,
  `acc` SMALLINT(6) NOT NULL,
  `avoid` SMALLINT(6) NOT NULL,
  `hands` SMALLINT(6) NOT NULL,
  `speed` SMALLINT(6) NOT NULL,
  `jump` SMALLINT(6) NOT NULL,
  PRIMARY KEY (`inventoryequipmentid`),
  KEY (`inventoryitemid`),
  CONSTRAINT FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `inventorymounts` (
  `inventorymountid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` INT(10) UNSIGNED NOT NULL,
  `level` TINYINT(2) UNSIGNED NOT NULL,
  `exp` SMALLINT(4) UNSIGNED NOT NULL,
  `tiredness` TINYINT(3) UNSIGNED NOT NULL,
  PRIMARY KEY (`inventorymountid`),
  KEY (`inventoryitemid`),
  CONSTRAINT FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `inventorypets` (
  `inventorypetid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` INT(10) UNSIGNED NOT NULL,
  `position` TINYINT(1) NOT NULL,
  `name` VARCHAR(13) NOT NULL,
  `level` TINYINT(2) NOT NULL,
  `closeness` SMALLINT(6) NOT NULL,
  `fullness` TINYINT(4) NOT NULL,
  `expired` TINYINT(1) NOT NULL,
  PRIMARY KEY (`inventorypetid`),
  KEY (`inventoryitemid`),
  CONSTRAINT FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `inventoryrings` (
  `inventoryringid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` INT(10) UNSIGNED NOT NULL,
  `partnerchrid` INT(11) NOT NULL,
  `partnerringid` BIGINT(20) NOT NULL,
  PRIMARY KEY (`inventoryringid`),
  KEY (`inventoryitemid`),
  CONSTRAINT FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `cashshoppurchases` (
  `uniqueid` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `inventoryitemid` INT(10) UNSIGNED DEFAULT NULL,
  `purchaseracctid` INT(11) DEFAULT NULL,
  `gifterchrname` VARCHAR(13) DEFAULT NULL,
  `serialnumber` INT(11) DEFAULT NULL,
  PRIMARY KEY (`uniqueid`),
  KEY (`inventoryitemid`),
  CONSTRAINT FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE SET NULL
) Engine=InnoDB;

CREATE TABLE `queststatuses` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `questid` SMALLINT(6) NOT NULL,
  `state` TINYINT(1) NOT NULL,
  `completed` BIGINT(20),
  PRIMARY KEY (`id`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `questmobprogress` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `queststatusid` INT(11) UNSIGNED NOT NULL,
  `mobid` INT(11) NOT NULL,
  `count` SMALLINT(4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`queststatusid`),
  CONSTRAINT FOREIGN KEY (`queststatusid`) REFERENCES `queststatuses` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE `buddyentries` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `owner` INT(11) NOT NULL,
  `buddy` INT(11) NOT NULL,
  `buddyname` VARCHAR(12) NOT NULL,
  `status` TINYINT(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`owner`),
  KEY (`buddy`),
  CONSTRAINT FOREIGN KEY (`owner`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `cooldowns` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `skillid` INT(11) NOT NULL,
  `remaining` SMALLINT(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `famelog` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `from` INT(11) NOT NULL,
  `to` INT(11) NOT NULL,
  `millis` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`from`),
  CONSTRAINT FOREIGN KEY (`from`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `guilds` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `world` TINYINT(2) NOT NULL,
  `name` VARCHAR(12) NOT NULL,
  `titles` VARCHAR(64) NOT NULL DEFAULT 'Master,Jr.Master,Member,,',
  `capacity` TINYINT(4) NOT NULL DEFAULT 10,
  `emblemBackground` SMALLINT(6) NOT NULL DEFAULT 0,
  `emblemBackgroundColor` TINYINT(4) NOT NULL DEFAULT 0,
  `emblemDesign` SMALLINT(6) NOT NULL DEFAULT 0,
  `emblemDesignColor` TINYINT(4) NOT NULL DEFAULT 0,
  `notice` VARCHAR(100) NOT NULL DEFAULT '',
  `gp` INT(11) NOT NULL DEfAULT 0,
  `alliance` INT(11) NOT NULL DEFAULT 0,
  `nextbbstopicid` INT(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `guildbbstopics` (
  `topicsid` INT(11) NOT NULL AUTO_INCREMENT,
  `guildid` INT(11) NOT NULL,
  `topicid` INT(11) NOT NULL,
  `poster` INT(11) NOT NULL,
  `posttime` BIGINT(20) NOT NULL,
  `subject` VARCHAR(25) NOT NULL,
  `content` VARCHAR(600) NOT NULL,
  `icon` INT(11) NOT NULL,
  `nextreplyid` INT(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`topicsid`),
  KEY (`guildid`),
  KEY (`guildid`,`topicid`),
  CONSTRAINT FOREIGN KEY (`poster`) REFERENCES `characters` (`id`) ON DELETE CASCADE,
  CONSTRAINT FOREIGN KEY (`guildid`) REFERENCES `guilds` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `guildbbsreplies` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `topicsid` INT(11) NOT NULL,
  `replyid` INT(11) NOT NULL,
  `poster` INT(11) NOT NULL,
  `posttime` BIGINT(20) NOT NULL,
  `content` VARCHAR(25) NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`topicsid`,`replyid`),
  CONSTRAINT FOREIGN KEY (`poster`) REFERENCES `characters` (`id`) ON DELETE CASCADE,
  CONSTRAINT FOREIGN KEY (`topicsid`) REFERENCES `guildbbstopics` (`topicsid`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `guildmembers` (
  `entryid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `guildid` INT(11) NOT NULL,
  `characterid` INT(11) NOT NULL,
  `rank` TINYINT(4) NOT NULL,
  `signature` TINYINT(4) NOT NULL,
  `alliancerank` TINYINT(4) NOT NULL,
  PRIMARY KEY (`entryid`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE,
  CONSTRAINT FOREIGN KEY (`guildid`) REFERENCES `guilds` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `keymaps` (
  `entryid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `key` TINYINT(3) NOT NULL,
  `type` TINYINT(1) NOT NULL,
  `action` INT(11) NOT NULL,
  PRIMARY KEY (`entryid`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `mapmemory` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `key` ENUM('FREE_MARKET','WORLD_TOUR','FLORINA','ARIANT','MONSTER_CARNIVAL','JAIL') NOT NULL,
  `value` INT(11) NOT NULL,
  `spawnpoint` TINYINT(4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `minigamescores` (
  `entryid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `gametype` TINYINT(1) NOT NULL,
  `wins` INT(11) NOT NULL DEFAULT 0,
  `ties` INT(11) NOT NULL DEFAULT 0,
  `losses` INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`entryid`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `parties` (
  `entryid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `world` TINYINT(2) NOT NULL,
  `partyid` INT(11) NOT NULL,
  `characterid` INT(11) NOT NULL,
  `leader` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`entryid`),
  KEY (`world`,`partyid`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `skills` (
  `entryid` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `skillid` INT(11) NOT NULL,
  `level` TINYINT(2) NOT NULL,
  `mastery` TINYINT(2),
  PRIMARY KEY (`entryid`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `skillmacros` (
  `entryid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `position` TINYINT(1) NOT NULL,
  `name` VARCHAR(12) NOT NULL,
  `silent` TINYINT(1) NOT NULL,
  `skill1` INT(11) NOT NULL,
  `skill2` INT(11) NOT NULL,
  `skill3` INT(11) NOT NULL,
  PRIMARY KEY (`entryid`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `wishlists` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `sn` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`characterid`),
  CONSTRAINT FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `bans` (
  `banid` INT(11) NOT NULL AUTO_INCREMENT,
  `accountid` INT(11) NOT NULL,
  `ip` INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (`banid`),
  KEY (`accountid`),
  KEY (`ip`)
) ENGINE = InnoDB;

CREATE TABLE `macbans` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `banid` INT(11) NOT NULL,
  `mac` BINARY(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY (`banid`),
  CONSTRAINT FOREIGN KEY (`banid`) REFERENCES `bans` (`banid`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `infractions` (
  `entryid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountid` INT(11) NOT NULL,
  `characterid` INT(11),
  `receivedate` BIGINT(20) NOT NULL,
  `expiredate` BIGINT(20) NOT NULL,
  `assignertype` ENUM('gm warning','machine detected') NOT NULL,
  `assignername` VARCHAR(255) NOT NULL,
  `assignercomment` VARCHAR(255) NOT NULL,
  `reason` TINYINT(3) NOT NULL,
  `severity` SMALLINT(6) NOT NULL,
  `pardoned` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`entryid`),
  KEY (`accountid`)
) ENGINE=InnoDB;

DROP PROCEDURE IF EXISTS `updateranks`;
DELIMITER $$
CREATE PROCEDURE `updateranks` (`type` ENUM('overall', 'world', 'job', 'fame'), `value` TINYINT(2))
BEGIN
  CASE `type`
    WHEN 'overall' THEN
      SET @lastrank := @rankcount := 0, @lastlevel := @lastexp := -1;
      UPDATE `characters` `target`
        INNER JOIN (
          SELECT DISTINCT `c`.`id`
          FROM `characters` `c`
          LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`
          LEFT JOIN `bans` `b` ON `a`.`id` = `b`.`accountid` OR `a`.`recentip` = `b`.`ip`
          WHERE (
            `a`.`gm` = 0
            AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))
            AND IF(ISNULL(@banexpire := (SELECT MAX(`expiredate`) FROM `infractions` WHERE `accountid` = `b`.`accountid`)), 0, @banexpire DIV 1000) < UNIX_TIMESTAMP()
          )
          ORDER BY
            `c`.`level` DESC,
            `c`.`exp` DESC
        ) AS `source` ON `source`.`id` = `target`.`id`
      SET
        `target`.`overallrankoldpos` = `target`.`overallrankcurrentpos`,
        `target`.`overallrankcurrentpos` = GREATEST(
          @lastrank := IF(`level` <> 200 AND @lastlevel = `level` AND @lastexp = `exp`, @lastrank, @rankcount + 1),
          LEAST(0, @rankcount := @rankcount + 1),
          LEAST(0, @lastlevel := `level`),
          LEAST(0, @lastexp := `exp`)
        )
      ;
    WHEN 'world' THEN
      SET @lastrank := @rankcount := 0, @lastlevel := @lastexp := -1;
      UPDATE `characters` `target`
        INNER JOIN (
          SELECT DISTINCT `c`.`id`
          FROM `characters` `c`
          LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`
          LEFT JOIN `bans` `b` ON `a`.`id` = `b`.`accountid` OR `a`.`recentip` = `b`.`ip`
          WHERE
            `c`.`world` = `value`
            AND `a`.`gm` = 0
            AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))
            AND IF(ISNULL(@banexpire := (SELECT MAX(`expiredate`) FROM `infractions` WHERE `accountid` = `b`.`accountid`)), 0, @banexpire DIV 1000) < UNIX_TIMESTAMP()
          ORDER BY
            `c`.`level` DESC,
            `c`.`exp` DESC
        ) AS `source` ON `source`.`id` = `target`.`id`
      SET
       `target`.`worldrankoldpos` = `target`.`worldrankcurrentpos`,
       `target`.`worldrankcurrentpos` = GREATEST(
          @lastrank := IF(`level` <> 200 AND @lastlevel = `level` AND @lastexp = `exp`, @lastrank, @rankcount + 1),
          LEAST(0, @rankcount := @rankcount + 1),
          LEAST(0, @lastlevel := `level`),
          LEAST(0, @lastexp := `exp`)
        )
      ;
    WHEN 'job' THEN
      SET @lastrank := @rankcount := 0, @lastlevel := @lastexp := -1;
      UPDATE `characters` `target`
        INNER JOIN (
          SELECT DISTINCT `c`.`id`
          FROM `characters` `c`
          LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`
          LEFT JOIN `bans` `b` ON `a`.`id` = `b`.`accountid` OR `a`.`recentip` = `b`.`ip`
          WHERE
            (`c`.`job` DIV 100) = `value`
            AND `a`.`gm` = 0
            AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))
            AND IF(ISNULL(@banexpire := (SELECT MAX(`expiredate`) FROM `infractions` WHERE `accountid` = `b`.`accountid`)), 0, @banexpire DIV 1000) < UNIX_TIMESTAMP()
          ORDER BY
            `c`.`level` DESC,
            `c`.`exp` DESC
        ) AS `source` ON `source`.`id` = `target`.`id`
      SET
       `target`.`jobrankoldpos` = `target`.`jobrankcurrentpos`,
       `target`.`jobrankcurrentpos` = GREATEST(
          @lastrank := IF(`level` <> 200 AND @lastlevel = `level` AND @lastexp = `exp`, @lastrank, @rankcount + 1),
          LEAST(0, @rankcount := @rankcount + 1),
          LEAST(0, @lastlevel := `level`),
          LEAST(0, @lastexp := `exp`)
        )
      ;
    WHEN 'fame' THEN
      SET @rankcount := 0;
      UPDATE `characters` `target`
        INNER JOIN (
          SELECT DISTINCT `c`.`id`
          FROM `characters` `c`
          LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`
          LEFT JOIN `bans` `b` ON `a`.`id` = `b`.`accountid` OR `a`.`recentip` = `b`.`ip`
          WHERE
            `c`.`fame` > 0
            AND `a`.`gm` = 0
            AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))
            AND IF(ISNULL(@banexpire := (SELECT MAX(`expiredate`) FROM `infractions` WHERE `accountid` = `b`.`accountid`)), 0, @banexpire DIV 1000) < UNIX_TIMESTAMP()
          ORDER BY
            `c`.`fame` DESC,
            `c`.`level` DESC,
            `c`.`exp` DESC
        ) AS `source` ON `source`.`id` = `target`.`id`
      SET
       `target`.`famerankoldpos` = `target`.`famerankcurrentpos`,
       `target`.`famerankcurrentpos` = (@rankcount := @rankcount + 1)
      ;
  END CASE;
END $$
DELIMITER ;

DROP PROCEDURE IF EXISTS `fetchranks`;
DELIMITER $$
CREATE PROCEDURE `fetchranks` (`type` ENUM('overall', 'world', 'job', 'fame'), `value` TINYINT(2), `lowerbound` INT(11), `upperbound` INT(11))
BEGIN
  CASE `type`
    WHEN 'overall' THEN
      SELECT `c`.`overallrankcurrentpos`,`c`.`id`,`c`.`name`,`c`.`world`,`c`.`job`,`c`.`level`,`c`.`exp` FROM `characters` `c`
      LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`
      LEFT JOIN `bans` `b` ON `a`.`id` = `b`.`accountid` OR `a`.`recentip` = `b`.`ip`
      WHERE
        (ISNULL(`lowerbound`) OR `c`.`overallrankcurrentpos` >= `lowerbound`)
        AND (ISNULL(`upperbound`) OR `c`.`overallrankcurrentpos` <= `upperbound`)
        AND `a`.`gm` = 0
        AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))
        AND IF(ISNULL(@banexpire := (SELECT MAX(`expiredate`) FROM `infractions` WHERE `accountid` = `b`.`accountid`)), 0, @banexpire DIV 1000) < UNIX_TIMESTAMP()
      ORDER BY `c`.`overallrankcurrentpos` ASC;
    WHEN 'world' THEN
      SELECT `c`.`worldrankcurrentpos`,`c`.`id`,`c`.`name`,`c`.`world`,`c`.`job`,`c`.`level`,`c`.`exp` FROM `characters` `c`
      LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`
      LEFT JOIN `bans` `b` ON `a`.`id` = `b`.`accountid` OR `a`.`recentip` = `b`.`ip`
      WHERE
        `c`.`world` = `value`
        AND (ISNULL(`lowerbound`) OR `c`.`worldrankcurrentpos` >= `lowerbound`)
        AND (ISNULL(`upperbound`) OR `c`.`worldrankcurrentpos` <= `upperbound`)
        AND `a`.`gm` = 0
        AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))
        AND IF(ISNULL(@banexpire := (SELECT MAX(`expiredate`) FROM `infractions` WHERE `accountid` = `b`.`accountid`)), 0, @banexpire DIV 1000) < UNIX_TIMESTAMP()
      ORDER BY `c`.`worldrankcurrentpos` ASC;
    WHEN 'job' THEN
      SELECT `c`.`jobrankcurrentpos`,`c`.`id`,`c`.`name`,`c`.`world`,`c`.`job`,`c`.`level`,`c`.`exp` FROM `characters` `c`
      LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`
      LEFT JOIN `bans` `b` ON `a`.`id` = `b`.`accountid` OR `a`.`recentip` = `b`.`ip`
      WHERE
        (`c`.`job` DIV 100) = `value`
        AND (ISNULL(`lowerbound`) OR `c`.`jobrankcurrentpos` >= `lowerbound`)
        AND (ISNULL(`upperbound`) OR `c`.`jobrankcurrentpos` <= `upperbound`)
        AND `a`.`gm` = 0
        AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))
        AND IF(ISNULL(@banexpire := (SELECT MAX(`expiredate`) FROM `infractions` WHERE `accountid` = `b`.`accountid`)), 0, @banexpire DIV 1000) < UNIX_TIMESTAMP()
      ORDER BY `c`.`jobrankcurrentpos` ASC;
    WHEN 'fame' THEN
      SELECT `c`.`famerankcurrentpos`,`c`.`id`,`c`.`name`,`c`.`world`,`c`.`job`,`c`.`fame`,`c`.`level`,`c`.`exp` FROM `characters` `c`
      LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`
      LEFT JOIN `bans` `b` ON `a`.`id` = `b`.`accountid` OR `a`.`recentip` = `b`.`ip`
      WHERE
        `c`.`fame` > 0
        AND (ISNULL(`lowerbound`) OR `c`.`famerankcurrentpos` >= `lowerbound`)
        AND (ISNULL(`upperbound`) OR `c`.`famerankcurrentpos` <= `upperbound`)
        AND `a`.`gm` = 0
        AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))
        AND IF(ISNULL(@banexpire := (SELECT MAX(`expiredate`) FROM `infractions` WHERE `accountid` = `b`.`accountid`)), 0, @banexpire DIV 1000) < UNIX_TIMESTAMP()
      ORDER BY `c`.`famerankcurrentpos` ASC;
  END CASE;
END $$
DELIMITER ;