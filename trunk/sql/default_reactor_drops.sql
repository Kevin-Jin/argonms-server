/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
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

--
-- This script needs to be executed only if MCDB is not being used.
--

DROP TABLE IF EXISTS `reactordrops`;

CREATE TABLE `reactordrops` (
  `reactordropid` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `reactorid` INT(11) NOT NULL,
  `itemid` INT(11) NOT NULL,
  `chance` INT(11) NOT NULL,
  PRIMARY KEY (`reactordropid`),
  KEY `reactorid` (`reactorid`)
) ENGINE=InnoDB;

/*!40000 ALTER TABLE `reactordrops` DISABLE KEYS */;
INSERT INTO `reactordrops` (`reactorid`,`itemid`,`chance`) VALUES
 (2000,4031161,1),
 (2000,4031162,1),
 (2000,2010009,2),
 (2000,2010000,4),
 (2000,2000000,4),
 (2000,2000001,7),
 (2000,2000002,10),
 (2000,2000003,15),
 (1012000,2000000,6),
 (1012000,4000003,6),
 (1012000,4031150,3),
 (1072000,4031165,4),
 (1102000,4000136,1),
 (1102001,4000136,1),
 (1102002,4000136,1),
 (2002000,2000002,4),
 (2002000,2000001,2),
 (2002000,4031198,2),
 (2112000,2000004,1),
 (2112001,2020001,1),
 (2112004,4001016,1),
 (2112005,4001015,1),
 (2112003,2000005,1),
 (2112007,2022001,1),
 (2112008,2000004,1),
 (2112009,2020001,1),
 (2112010,2000005,1),
 (2112011,4001016,1),
 (2112012,4001015,1),
 (2112014,4001018,1),
 (2112016,4001113,1),
 (2112017,4001114,1),
 (2202000,4031094,1),
 (2212000,4031142,2),
 (2212000,2000002,3),
 (2212001,2000002,3),
 (2212002,2000002,3),
 (2212001,4031141,2),
 (2212002,4031143,2),
 (2212003,4031107,2),
 (2212004,4031116,2),
 (2212004,2000001,2),
 (2212005,4031136,8),
 (2222000,4031231,2),
 (2222000,4031258,2),
 (2222000,2000002,3),
 (2302000,2000001,3),
 (2302000,2022040,6),
 (2302000,4031274,50),
 (2302000,4031275,50),
 (2302000,4031276,50),
 (2302000,4031277,50),
 (2302000,4031278,50),
 (2302001,2000002,3),
 (2302001,2022040,4),
 (2302002,2000001,3),
 (2302002,2022040,8),
 (2302003,4161017,1),
 (2302005,4031508,1),
 (2502000,2022116,1),
 (2052001,2022116,1),
 (9202000,1032033,1),
 (9202009,1032033,1),
 (2202001,4031092,1);
/*!40000 ALTER TABLE `reactordrops` ENABLE KEYS */;