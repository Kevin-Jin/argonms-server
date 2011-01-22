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
  `name` varchar(12) DEFAULT NULL,
  `password` varchar(128) DEFAULT NULL,
  `salt` varchar(32) DEFAULT NULL,
  `pin` char(4) DEFAULT NULL,
  `gender` tinyint(1) DEFAULT NULL, /*NOT NULL DEFAULT 10*/
  `birthday` int(8) DEFAULT NULL, /*NOT NULL DEFAULT 0*/
  `characters` tinyint(1) DEFAULT NULL, /*NOT NULL DEFAULT 3*/
  `connected` tinyint(1) NOT NULL DEFAULT 0,
  `banexpire` int(11) UNSIGNED DEFAULT NULL,
  `banreason` tinyint(3) DEFAULT NULL,
  `banmessage` varchar(255) DEFAULT NULL,
  `gm` tinyint(3) DEFAULT NULL,
  PRIMARY KEY (`id`)
)
ENGINE = InnoDB;

DROP TABLE IF EXISTS `characters`;
CREATE TABLE `characters` (
  `accountid` int(11) NOT NULL,
  `world` tinyint(2) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(12) NOT NULL,
  `gender` tinyint(1) NOT NULL,
  `skin` tinyint(3) NOT NULL,
  `eyes` smallint(5) NOT NULL, /* too small? */
  `hair` smallint(5) NOT NULL, /* too small? */
  `level` tinyint(3) UNSIGNED NOT NULL,
  `job` smallint(5) NOT NULL,
  `str` smallint(5) NOT NULL,
  `dex` smallint(5) NOT NULL,
  `int` smallint(5) NOT NULL,
  `luk` smallint(5) NOT NULL,
  `hp` smallint(5) NOT NULL,
  `maxhp` smallint(5) NOT NULL,
  `mp` smallint(5) NOT NULL,
  `maxmp` smallint(5) NOT NULL,
  `ap` smallint(5) NOT NULL,
  `sp` smallint(5) NOT NULL,
  `exp` int(11) NOT NULL,
  `fame` smallint(5) NOT NULL,
  `spouse` int(11) NOT NULL,
  `map` int(11) NOT NULL,
  `spawnpoint` tinyint(3) NOT NULL,
  `mesos` int(11) NOT NULL,
  `equipslots` tinyint(3) UNSIGNED NOT NULL,
  `useslots` tinyint(3) UNSIGNED NOT NULL,
  `setupslots` tinyint(3) UNSIGNED NOT NULL,
  `etcslots` tinyint(3) UNSIGNED NOT NULL,
  `cashslots` tinyint(3) UNSIGNED NOT NULL,
  `storageslots` tinyint(3) UNSIGNED NOT NULL,
  `gm` tinyint(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `accountid` (`accountid`),
  CONSTRAINT `characters_ibfk_1` FOREIGN KEY (`accountid`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=30000 DEFAULT CHARSET=latin1;