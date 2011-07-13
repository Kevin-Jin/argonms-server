/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
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

DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(12) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `salt` VARCHAR(32) DEFAULT NULL,
  `pin` CHAR(4) DEFAULT NULL,
  `gender` TINYINT(1) NOT NULL DEFAULT 10,
  `birthday` INT(8) DEFAULT NULL,
  `characters` TINYINT(1) NOT NULL DEFAULT 3,
  `connected` TINYINT(1) NOT NULL DEFAULT 0,
  `banexpire` INT(11) UNSIGNED DEFAULT NULL,
  `banreason` TINYINT(3) DEFAULT NULL,
  `banmessage` VARCHAR(255) DEFAULT NULL,
  `storageslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 4,
  `storagemesos` INT(11) NOT NULL DEFAULT 0,
  `gm` TINYINT(3) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS `characters`;
CREATE TABLE `characters` (
  `accountid` INT(11) NOT NULL,
  `world` TINYINT(2) NOT NULL,
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(12) NOT NULL,
  `gender` TINYINT(1) NOT NULL,
  `skin` TINYINT(3) NOT NULL,
  `eyes` SMALLINT(5) NOT NULL,
  `hair` SMALLINT(5) NOT NULL,
  `level` TINYINT(3) UNSIGNED NOT NULL DEFAULT 1,
  `job` SMALLINT(5) NOT NULL DEFAULT 0,
  `str` SMALLINT(5) NOT NULL,
  `dex` SMALLINT(5) NOT NULL,
  `int` SMALLINT(5) NOT NULL,
  `luk` SMALLINT(5) NOT NULL,
  `hp` SMALLINT(5) NOT NULL DEFAULT 50,
  `maxhp` SMALLINT(5) NOT NULL DEFAULT 50,
  `mp` SMALLINT(5) NOT NULL DEFAULT 50,
  `maxmp` SMALLINT(5) NOT NULL DEFAULT 50,
  `ap` SMALLINT(5) NOT NULL DEFAULT 0,
  `sp` SMALLINT(5) NOT NULL DEFAULT 0,
  `exp` INT(11) NOT NULL DEFAULT 0,
  `fame` SMALLINT(5) NOT NULL DEFAULT 0,
  `spouse` INT(11) NOT NULL DEFAULT 0,
  `map` INT(11) NOT NULL DEFAULT 0,
  `spawnpoint` TINYINT(3) NOT NULL DEFAULT 0,
  `mesos` INT(11) NOT NULL DEFAULT 0,
  `equipslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `useslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `setupslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `etcslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `cashslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 24,
  `buddyslots` TINYINT(3) UNSIGNED NOT NULL DEFAULT 20,
  `gm` TINYINT(3) NOT NULL,
  `overallrankcurrentpos` INT(11) NOT NULL DEFAULT 0,
  `overallrankoldpos` INT(11) NOT NULL DEFAULT 0,
  `worldrankcurrentpos` INT(11) NOT NULL DEFAULT 0,
  `worldrankoldpos` INT(11) NOT NULL DEFAULT 0,
  `jobrankcurrentpos` INT(11) NOT NULL DEFAULT 0,
  `jobrankoldpos` INT(11) NOT NULL DEFAULT 0,
  `famerankcurrentpos` INT(11) NOT NULL DEFAULT 0,
  `famerankoldpos` INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `accountid` (`accountid`),
  CONSTRAINT `characters_ibfk_1` FOREIGN KEY (`accountid`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

DROP TABLE IF EXISTS `cooldowns`;
CREATE TABLE `cooldowns` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `skillid` INT(11) NOT NULL,
  `remaining` SMALLINT(5) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `cooldowns_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventoryitems`;
CREATE TABLE `inventoryitems` (
  `inventoryitemid` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) DEFAULT NULL,
  `accountid` INT(11) DEFAULT NULL,
  `inventorytype` TINYINT(1) DEFAULT NULL,
  `position` SMALLINT(5) NOT NULL,
  `itemid` INT(11) NOT NULL,
  `expiredate` BIGINT(20) UNSIGNED NOT NULL,
  `uniqueid` BIGINT(20) UNSIGNED NOT NULL,
  `owner` TINYTEXT DEFAULT NULL,
  `quantity` SMALLINT(5) NOT NULL,
  PRIMARY KEY (`inventoryitemid`),
  KEY `characterid` (`characterid`),
  KEY `accountid` (`accountid`),
  KEY `uniqueid` (`uniqueid`),
  CONSTRAINT `inventoryitems_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE,
  CONSTRAINT `inventoryitems_ibfk_2` FOREIGN KEY (`accountid`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventoryequipment`;
CREATE TABLE `inventoryequipment` (
  `inventoryequipmentid` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` INT(11) UNSIGNED NOT NULL,
  `str` SMALLINT(5) NOT NULL,
  `dex` SMALLINT(5) NOT NULL,
  `int` SMALLINT(5) NOT NULL,
  `luk` SMALLINT(5) NOT NULL,
  `hp` SMALLINT(5) NOT NULL,
  `mp` SMALLINT(5) NOT NULL,
  `watk` SMALLINT(5) NOT NULL,
  `matk` SMALLINT(5) NOT NULL,
  `wdef` SMALLINT(5) NOT NULL,
  `mdef` SMALLINT(5) NOT NULL,
  `acc` SMALLINT(5) NOT NULL,
  `avoid` SMALLINT(5) NOT NULL,
  `speed` SMALLINT(5) NOT NULL,
  `jump` SMALLINT(5) NOT NULL,
  `upgradeslots` TINYINT(3) NOT NULL,
  PRIMARY KEY (`inventoryequipmentid`),
  KEY `inventoryitemid` (`inventoryitemid`),
  CONSTRAINT `inventoryequipment_ibfk_1` FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventorymounts`;
CREATE TABLE `inventorymounts` (
  `inventorymountid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` INT(10) UNSIGNED NOT NULL,
  `level` TINYINT(2) UNSIGNED NOT NULL,
  `exp` SMALLINT(4) UNSIGNED NOT NULL,
  `tiredness` TINYINT(3) UNSIGNED NOT NULL,
  PRIMARY KEY (`inventorymountid`),
  KEY `inventoryitemid` (`inventoryitemid`),
  CONSTRAINT `inventorymounts_ibfk_1` FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventorypets`;
CREATE TABLE `inventorypets` (
  `inventorypetid` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` INT(11) UNSIGNED NOT NULL,
  `position` TINYINT(1) NOT NULL,
  `name` VARCHAR(13) NOT NULL,
  `level` TINYINT(2) NOT NULL,
  `closeness` SMALLINT(5) NOT NULL,
  `fullness` TINYINT(3) NOT NULL,
  `expired` TINYINT(1) NOT NULL,
  PRIMARY KEY (`inventorypetid`),
  KEY `inventoryitemid` (`inventoryitemid`),
  CONSTRAINT `inventorypets_ibfk_1` FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventoryrings`;
CREATE TABLE `inventoryrings` (
  `inventoryringid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` INT(10) UNSIGNED NOT NULL,
  `partnerchrid` INT(11) NOT NULL,
  `partnerringid` BIGINT(20) NOT NULL,
  PRIMARY KEY (`inventoryringid`),
  KEY `inventoryitemid` (`inventoryitemid`),
  CONSTRAINT `inventoryrings_ibfk_1` FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `keymaps`;
CREATE TABLE `keymaps` (
  `entryid` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `key` TINYINT(3) NOT NULL,
  `type` TINYINT(1) NOT NULL,
  `action` INT(11) NOT NULL,
  PRIMARY KEY(`entryid`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `keymaps_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `minigamescores`;
CREATE TABLE `minigamescores` (
  `entryid` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `gametype` TINYINT(3) NOT NULL,
  `wins` INT(11) NOT NULL DEFAULT 0,
  `ties` INT(11) NOT NULL DEFAULT 0,
  `losses` INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY(`entryid`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `minigamescores_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `skillmacros`;
CREATE TABLE `skillmacros` (
  `entryid` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `position` TINYINT(1) NOT NULL,
  `name` TINYTEXT NOT NULL,
  `shout` TINYINT(1) NOT NULL,
  `skill1` INT(11) NOT NULL,
  `skill2` INT(11) NOT NULL,
  `skill3` INT(11) NOT NULL,
  PRIMARY KEY(`entryid`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `skillmacros_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `macbans`;
CREATE TABLE `macbans` (
  `mac` tinytext NOT NULL
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `queststatuses`;
CREATE TABLE `queststatuses` (
  `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `questid` SMALLINT(5) NOT NULL,
  `state` TINYINT(1) NOT NULL,
  `completed` BIGINT(20),
  PRIMARY KEY(`id`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `queststatuses_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `questmobprogress`;
CREATE TABLE `questmobprogress` (
  `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `queststatusid` INT(11) UNSIGNED NOT NULL,
  `mobid` INT(11) NOT NULL,
  `count` SMALLINT(3) NOT NULL,
  PRIMARY KEY(`id`),
  KEY `queststatusid` (`queststatusid`),
  CONSTRAINT `questmobprogress_ibfk_1` FOREIGN KEY (`queststatusid`) REFERENCES `queststatuses` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB;

DROP TABLE IF EXISTS `skills`;
CREATE TABLE `skills` (
  `entryid` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` INT(11) NOT NULL,
  `skillid` INT(11) NOT NULL,
  `level` TINYINT(2) NOT NULL,
  `mastery` TINYINT(2) DEFAULT NULL,
  PRIMARY KEY (`entryid`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `skills_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `uniqueid`;
CREATE TABLE `uniqueid` (
  `nextuid` BIGINT(20) NOT NULL DEFAULT 1,
  PRIMARY KEY (`nextuid`)
) ENGINE=InnoDB;