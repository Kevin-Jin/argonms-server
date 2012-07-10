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
-- This SQL needs to be executed only if MCDB is not being used.
--

DROP TABLE IF EXISTS `reactordrops`;
CREATE TABLE `reactordrops` (
  `reactordropid` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `reactorid` int(11) NOT NULL,
  `itemid` int(11) NOT NULL,
  `chance` int(11) NOT NULL,
  PRIMARY KEY (`reactordropid`),
  KEY `reactorid` (`reactorid`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=latin1;

/*!40000 ALTER TABLE `reactordrops` DISABLE KEYS */;
INSERT INTO `reactordrops` (`reactordropid`,`reactorid`,`itemid`,`chance`) VALUES
 (1,2000,4031161,1),
 (2,2000,4031162,1),
 (3,2000,2010009,2),
 (4,2000,2010000,4),
 (5,2000,2000000,4),
 (6,2000,2000001,7),
 (7,2000,2000002,10),
 (8,2000,2000003,15),
 (9,1012000,2000000,6),
 (10,1012000,4000003,6),
 (11,1012000,4031150,3),
 (12,1072000,4031165,4),
 (13,1102000,4000136,1),
 (14,1102001,4000136,1),
 (15,1102002,4000136,1),
 (16,2002000,2000002,4),
 (17,2002000,2000001,2),
 (18,2002000,4031198,2),
 (19,2112000,2000004,1),
 (20,2112001,2020001,1),
 (21,2112004,4001016,1),
 (22,2112005,4001015,1),
 (23,2112003,2000005,1),
 (24,2112007,2022001,1),
 (25,2112008,2000004,1),
 (26,2112009,2020001,1),
 (27,2112010,2000005,1),
 (28,2112011,4001016,1),
 (29,2112012,4001015,1),
 (30,2112014,4001018,1),
 (31,2112016,4001113,1),
 (32,2112017,4001114,1),
 (33,2202000,4031094,1),
 (34,2212000,4031142,2),
 (35,2212000,2000002,3),
 (36,2212001,2000002,3),
 (37,2212002,2000002,3),
 (38,2212001,4031141,2),
 (39,2212002,4031143,2),
 (40,2212003,4031107,2),
 (41,2212004,4031116,2),
 (42,2212004,2000001,2),
 (43,2212005,4031136,8),
 (44,2222000,4031231,2),
 (45,2222000,4031258,2),
 (46,2222000,2000002,3),
 (47,2302000,2000001,3),
 (48,2302000,2022040,6),
 (49,2302000,4031274,50),
 (50,2302000,4031275,50),
 (51,2302000,4031276,50),
 (52,2302000,4031277,50),
 (53,2302000,4031278,50),
 (54,2302001,2000002,3),
 (55,2302001,2022040,4),
 (56,2302002,2000001,3),
 (57,2302002,2022040,8),
 (58,2302003,4161017,1),
 (59,2302005,4031508,1),
 (60,2502000,2022116,1),
 (61,2052001,2022116,1),
 (62,9202000,1032033,1),
 (63,9202009,1032033,1),
 (64,2202001,4031092,1);
/*!40000 ALTER TABLE `reactordrops` ENABLE KEYS */;