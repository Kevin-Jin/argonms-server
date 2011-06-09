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
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(12) NOT NULL,
  `password` varchar(128) NOT NULL,
  `salt` varchar(32) DEFAULT NULL,
  `pin` char(4) DEFAULT NULL,
  `gender` tinyint(1) NOT NULL DEFAULT 10,
  `birthday` int(8) DEFAULT NULL,
  `characters` tinyint(1) NOT NULL DEFAULT 3,
  `connected` tinyint(1) NOT NULL DEFAULT 0,
  `banexpire` int(11) UNSIGNED DEFAULT NULL,
  `banreason` tinyint(3) DEFAULT NULL,
  `banmessage` varchar(255) DEFAULT NULL,
  `gm` tinyint(3) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS `characters`;
CREATE TABLE `characters` (
  `accountid` int(11) NOT NULL,
  `world` tinyint(2) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(12) NOT NULL,
  `gender` tinyint(1) NOT NULL,
  `skin` tinyint(3) NOT NULL,
  `eyes` smallint(5) NOT NULL,
  `hair` smallint(5) NOT NULL,
  `level` tinyint(3) UNSIGNED NOT NULL DEFAULT 1,
  `job` smallint(5) NOT NULL DEFAULT 0,
  `str` smallint(5) NOT NULL,
  `dex` smallint(5) NOT NULL,
  `int` smallint(5) NOT NULL,
  `luk` smallint(5) NOT NULL,
  `hp` smallint(5) NOT NULL DEFAULT 50,
  `maxhp` smallint(5) NOT NULL DEFAULT 50,
  `mp` smallint(5) NOT NULL DEFAULT 50,
  `maxmp` smallint(5) NOT NULL DEFAULT 50,
  `ap` smallint(5) NOT NULL DEFAULT 0,
  `sp` smallint(5) NOT NULL DEFAULT 0,
  `exp` int(11) NOT NULL DEFAULT 0,
  `fame` smallint(5) NOT NULL DEFAULT 0,
  `spouse` int(11) NOT NULL DEFAULT 0,
  `map` int(11) NOT NULL DEFAULT 0,
  `spawnpoint` tinyint(3) NOT NULL DEFAULT 0,
  `mesos` int(11) NOT NULL DEFAULT 0,
  `equipslots` tinyint(3) UNSIGNED NOT NULL DEFAULT 24,
  `useslots` tinyint(3) UNSIGNED NOT NULL DEFAULT 24,
  `setupslots` tinyint(3) UNSIGNED NOT NULL DEFAULT 24,
  `etcslots` tinyint(3) UNSIGNED NOT NULL DEFAULT 24,
  `cashslots` tinyint(3) UNSIGNED NOT NULL DEFAULT 24,
  `storageslots` tinyint(3) UNSIGNED NOT NULL DEFAULT 4,
  `buddyslots` tinyint(3) UNSIGNED NOT NULL DEFAULT 20,
  `gm` tinyint(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `accountid` (`accountid`),
  CONSTRAINT `characters_ibfk_1` FOREIGN KEY (`accountid`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

DROP TABLE IF EXISTS `cooldowns`;
CREATE TABLE `cooldowns` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `characterid` int(11) NOT NULL,
  `skillid` int(11) NOT NULL,
  `remaining` smallint(5) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `cooldowns_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventoryitems`;
CREATE TABLE `inventoryitems` (
  `inventoryitemid` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` int(11) DEFAULT NULL,
  `accountid` int(11) DEFAULT NULL,
  `inventorytype` tinyint(1) DEFAULT NULL,
  `position` smallint(5) NOT NULL,
  `itemid` int(11) NOT NULL,
  `expiredate` bigint(20) UNSIGNED NOT NULL,
  `uniqueid` bigint(20) UNSIGNED NOT NULL,
  `owner` tinytext DEFAULT NULL,
  `quantity` smallint(5) NOT NULL,
  PRIMARY KEY (`inventoryitemid`),
  KEY `characterid` (`characterid`),
  KEY `accountid` (`accountid`),
  KEY `uniqueid` (`uniqueid`),
  CONSTRAINT `inventoryitems_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE,
  CONSTRAINT `inventoryitems_ibfk_2` FOREIGN KEY (`accountid`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventoryequipment`;
CREATE TABLE `inventoryequipment` (
  `inventoryequipmentid` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` int(11) UNSIGNED NOT NULL,
  `str` smallint(5) NOT NULL,
  `dex` smallint(5) NOT NULL,
  `int` smallint(5) NOT NULL,
  `luk` smallint(5) NOT NULL,
  `hp` smallint(5) NOT NULL,
  `mp` smallint(5) NOT NULL,
  `watk` smallint(5) NOT NULL,
  `matk` smallint(5) NOT NULL,
  `wdef` smallint(5) NOT NULL,
  `mdef` smallint(5) NOT NULL,
  `acc` smallint(5) NOT NULL,
  `avoid` smallint(5) NOT NULL,
  `speed` smallint(5) NOT NULL,
  `jump` smallint(5) NOT NULL,
  `upgradeslots` tinyint(3) NOT NULL,
  PRIMARY KEY (`inventoryequipmentid`),
  KEY `inventoryitemid` (`inventoryitemid`),
  CONSTRAINT `inventoryequipment_ibfk_1` FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventorymounts`;
CREATE TABLE `inventorymounts` (
  `inventorymountid` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` int(10) UNSIGNED NOT NULL,
  `level` tinyint(2) UNSIGNED NOT NULL,
  `exp` smallint(4) UNSIGNED NOT NULL,
  `tiredness` tinyint(3) UNSIGNED NOT NULL,
  PRIMARY KEY (`inventorymountid`),
  KEY `inventoryitemid` (`inventoryitemid`),
  CONSTRAINT `inventorymounts_ibfk_1` FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventorypets`;
CREATE TABLE `inventorypets` (
  `inventorypetid` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` int(11) UNSIGNED NOT NULL,
  `position` tinyint(1) NOT NULL,
  `name` varchar(13) NOT NULL,
  `level` tinyint(2) NOT NULL,
  `closeness` smallint(5) NOT NULL,
  `fullness` tinyint(3) NOT NULL,
  `expired` tinyint(1) NOT NULL,
  PRIMARY KEY (`inventorypetid`),
  KEY `inventoryitemid` (`inventoryitemid`),
  CONSTRAINT `inventorypets_ibfk_1` FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `inventoryrings`;
CREATE TABLE `inventoryrings` (
  `inventoryringid` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventoryitemid` int(10) UNSIGNED NOT NULL,
  `partnerchrid` int(11) NOT NULL,
  `partnerringid` bigint(20) NOT NULL,
  PRIMARY KEY (`inventoryringid`),
  KEY `inventoryitemid` (`inventoryitemid`),
  CONSTRAINT `inventoryrings_ibfk_1` FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `keymaps`;
CREATE TABLE `keymaps` (
  `entryid` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` int(11) NOT NULL,
  `key` tinyint(3) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `action` int(11) NOT NULL,
  PRIMARY KEY(`entryid`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `keymaps_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `minigamescores`;
CREATE TABLE `minigamescores` (
  `entryid` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` int(11) NOT NULL,
  `gametype` tinyint(3) NOT NULL,
  `wins` int(11) NOT NULL DEFAULT 0,
  `ties` int(11) NOT NULL DEFAULT 0,
  `losses` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY(`entryid`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `minigamescores_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `skillmacros`;
CREATE TABLE `skillmacros` (
  `entryid` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` int(11) NOT NULL,
  `position` tinyint(1) NOT NULL,
  `name` tinytext NOT NULL,
  `shout` tinyint(1) NOT NULL,
  `skill1` int(11) NOT NULL,
  `skill2` int(11) NOT NULL,
  `skill3` int(11) NOT NULL,
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
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` int(11) NOT NULL,
  `questid` smallint(5) NOT NULL,
  `state` tinyint(1) NOT NULL,
  `completed` bigint(20),
  PRIMARY KEY(`id`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `queststatuses_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `questmobprogress`;
CREATE TABLE `questmobprogress` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `queststatusid` int(11) UNSIGNED NOT NULL,
  `mobid` int(11) NOT NULL,
  `count` smallint(3) NOT NULL,
  PRIMARY KEY(`id`),
  KEY `queststatusid` (`queststatusid`),
  CONSTRAINT `questmobprogress_ibfk_1` FOREIGN KEY (`queststatusid`) REFERENCES `queststatuses` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB;

DROP TABLE IF EXISTS `skills`;
CREATE TABLE `skills` (
  `entryid` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `characterid` int(11) NOT NULL,
  `skillid` int(11) NOT NULL,
  `level` tinyint(2) NOT NULL,
  `mastery` tinyint(2) DEFAULT NULL,
  PRIMARY KEY (`entryid`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `skills_ibfk_1` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;