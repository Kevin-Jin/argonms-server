let party;

function init(attachment) {
	party = attachment;
	party.loseItem(4001008);
	party.loseItem(4001007);
	party.changeMap(103000800, "st00");
	event.getMap(103000800).showTimer(30 * 60);
	event.startTimer("kick", 30 * 60 * 1000);
}

function playerChangedMap(playerId, map) {
	if (party.getLeader() == playerId && map == 103000890) {
		//leader has either died and respawned or left bonus stage
	}
}

function timerExpired(key) {
	switch (key) {
		case "kick":
			
			break;
	}
}