function untilNextQuarterHour() {
	let now = new Date();
	return ((15 - now.getMinutes() % 15) * 60 - now.getSeconds()) * 1000 - now.getMilliseconds();
}