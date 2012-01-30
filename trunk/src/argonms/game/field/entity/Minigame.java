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

package argonms.game.field.entity;

import argonms.common.net.external.ClientSendOps;
import argonms.common.util.Rng;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.character.GameCharacter;
import java.util.Random;

/**
 *
 * @author GoldenKevin
 */
public abstract class Minigame extends Miniroom {
	public enum MinigameResult {
		WIN(0),
		TIE(1),
		LOSS(2);

		private final byte byteValue;

		private MinigameResult(int intValue) {
			this.byteValue = (byte) intValue;
		}

		public byte byteValue() {
			return byteValue;
		}
	}

	private final boolean[] exitAfterFinish;
	protected boolean inProgress, ownerMovesFirst;
	protected byte currentPos;

	public Minigame(GameCharacter owner, String text, String password, byte pieceType) {
		super(owner, 2, text, password, pieceType);
		openToMap = true;
		exitAfterFinish = new boolean[2];
		inProgress = false;
		ownerMovesFirst = true;
		currentPos = 0;
	}

	@Override
	public void leaveRoom(GameCharacter p) {
		byte pos = positionOf(p);
		exitAfterFinish[pos] = false;
		if (inProgress)
			endGame(MinigameResult.LOSS, (byte) (pos == 0 ? 1 : 0));
		super.leaveRoom(p);
	}

	public void banVisitor() {
		banVisitor((byte) 1);
	}

	@Override
	public boolean gameInProgress() {
		return inProgress;
	}

	public void startGame() {
		reset();
		sendToAll(getStartMessage());

		inProgress = true;
		getPlayerByPosition((byte) 0).getMap().sendToAll(getUpdateBalloonMessage());
	}

	public void endGame(MinigameResult result, byte winnerPos) {
		GameCharacter p;
		switch (result) {
			case WIN:
			case LOSS:
				p = getPlayerByPosition(winnerPos);
				p.setMinigamePoints(getMiniroomType(), MinigameResult.WIN, p.getMinigamePoints(getMiniroomType(), MinigameResult.WIN) + 1);
				p = getPlayerByPosition((byte) (winnerPos == 0 ? 1 : 0));
				p.setMinigamePoints(getMiniroomType(), MinigameResult.LOSS, p.getMinigamePoints(getMiniroomType(), MinigameResult.LOSS) + 1);
				break;
			case TIE:
				p = getPlayerByPosition((byte) 0);
				p.setMinigamePoints(getMiniroomType(), MinigameResult.TIE, p.getMinigamePoints(getMiniroomType(), MinigameResult.TIE) + 1);
				p = getPlayerByPosition((byte) 1);
				p.setMinigamePoints(getMiniroomType(), MinigameResult.TIE, p.getMinigamePoints(getMiniroomType(), MinigameResult.TIE) + 1);
				break;
		}
		sendToAll(getFinishMessage(result, winnerPos));

		inProgress = false;
		byte exiterPos;
		if (!exitAfterFinish[exiterPos = 0] && !exitAfterFinish[exiterPos = 1]) {
			ownerMovesFirst = (result != MinigameResult.TIE ? winnerPos == 1 : !ownerMovesFirst);
			currentPos = (byte) (ownerMovesFirst ? 0 : 1);
			getPlayerByPosition((byte) 0).getMap().sendToAll(getUpdateBalloonMessage());
		} else {
			GameCharacter leaver = getPlayerByPosition(exiterPos);
			leaver.setMiniRoom(null);
			leaveRoom(leaver);
			leaver.getClient().getSession().send(getFirstPersonLeaveMessage(exiterPos, Miniroom.EXIT_SELF_SELECTED));
		}
	}

	public byte nextTurn() {
		return (currentPos = (byte) (currentPos == 0 ? 1 : 0));
	}

	public void setExitAfterGame(GameCharacter p, boolean shouldExit) {
		exitAfterFinish[positionOf(p)] = shouldExit;
	}

	protected abstract void reset();
	protected abstract byte[] getStartMessage();

	protected byte[] getFinishMessage(MinigameResult result, byte winnerPos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_FINISH_GAME);
		lew.writeByte(result.byteValue());
		if (result != MinigameResult.TIE)
			lew.writeByte(winnerPos);
		writeMinigameScores(lew, getPlayerByPosition((byte) 0), getMiniroomType());
		writeMinigameScores(lew, getPlayerByPosition((byte) 1), getMiniroomType());
		return lew.getBytes();
	}

	@Override
	public byte[] getFirstPersonJoinMessage(GameCharacter p) {
		GameCharacter v;

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_JOIN);
		lew.writeByte(getMiniroomType().byteValue());
		lew.writeByte(getMaxPlayers());
		lew.writeByte(positionOf(p));

		for (byte i = 0; i < getMaxPlayers(); i++)
			if ((v = getPlayerByPosition(i)) != null)
				writeMiniroomAvatar(lew, v, i);
		lew.writeByte((byte) 0xFF);

		for (byte i = 0; i < getMaxPlayers(); i++) {
			if ((v = getPlayerByPosition(i)) != null) {
				lew.writeByte(i);
				writeMinigameScores(lew, v, getMiniroomType());
			}
		}
		lew.writeByte((byte) 0xFF);

		lew.writeLengthPrefixedString(getMessage());
		lew.writeByte(getStyle());
		lew.writeByte((byte) 0);
		return lew.getBytes();
	}

	@Override
	public byte[] getThirdPersonJoinMessage(GameCharacter p, byte pos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_VISIT);
		writeMiniroomAvatar(lew, p, pos);
		writeMinigameScores(lew, p, getMiniroomType());
		return lew.getBytes();
	}

	public static class Omok extends Minigame {
		private enum MoveResult { MOVE, MOVE_AND_WIN, MOVE_AND_TIE, OVERLINE, DOUBLE_THREES}

		private static final int ROWS = 15;
		private static final int COLUMNS = 15;

		private final byte[][] board;
		private int[] lastLastMove, lastMove;

		public Omok(GameCharacter owner, String text, String password, byte stoneLook) {
			super(owner, text, password, stoneLook);
			board = new byte[COLUMNS][ROWS];
		}

		private MoveResult getResult(int x, int y, byte playerNum) {
			int horizontal = 1, vertical = 1, mainDiagonal = 1, antiDiagonal = 1;
			int nextX, nextY;

			for (nextX = x + 1; nextX < COLUMNS && board[nextX][y] == playerNum; nextX++)
				horizontal++;
			for (nextX = x - 1; nextX >= 0 && board[nextX][y] == playerNum; nextX--)
				horizontal++;

			for (nextY = y + 1; nextY < ROWS && board[x][nextY] == playerNum; nextY++)
				vertical++;
			for (nextY = y - 1; nextY >= 0 && board[x][nextY] == playerNum; nextY--)
				vertical++;

			for (nextX = x + 1, nextY = y + 1; nextX < COLUMNS && nextY < ROWS && board[nextX][nextY] == playerNum; nextX++, nextY++)
				mainDiagonal++;
			for (nextX = x - 1, nextY = y - 1; nextX >= 0 && nextY >= 0 && board[nextX][nextY] == playerNum; nextX--, nextY--)
				mainDiagonal++;

			for (nextX = x + 1, nextY = y - 1; nextX < COLUMNS && nextY >= 0 && board[nextX][nextY] == playerNum; nextX++, nextY--)
				antiDiagonal++;
			for (nextX = x - 1, nextY = y + 1; nextX >= 0 && nextY < ROWS && board[nextX][nextY] == playerNum; nextX--, nextY++)
				antiDiagonal++;

			if (horizontal > 5 || vertical > 5 || mainDiagonal > 5 || antiDiagonal > 5)
				return MoveResult.OVERLINE;
			if (horizontal == 3 && (vertical == 3 || mainDiagonal == 3 || antiDiagonal == 3)
					|| vertical == 3 && (mainDiagonal == 3 || antiDiagonal == 3)
					|| mainDiagonal == 3 && antiDiagonal == 3)
				return MoveResult.DOUBLE_THREES;

			lastLastMove = lastMove;
			lastMove = new int[] { x, y };
			board[x][y] = playerNum;
			if (horizontal == 5 || vertical == 5 || mainDiagonal == 5 || antiDiagonal == 5)
				return MoveResult.MOVE_AND_WIN;
			for (int i = 0; i < COLUMNS; i++)
				for (int j = 0; j < ROWS; j++)
					if (board[i][j] == 0)
						return MoveResult.MOVE;
			return MoveResult.MOVE_AND_TIE;
		}

		public void setPiece(GameCharacter p, int x, int y, byte playerNum) {
			switch (getResult(x, y, playerNum)) {
				case MOVE:
					currentPos = (byte) (positionOf(p) == 0 ? 1 : 0);
					sendToAll(writeMove(x, y, playerNum));
					break;
				case MOVE_AND_WIN:
					sendToAll(writeMove(x, y, playerNum));
					endGame(MinigameResult.WIN, positionOf(p));
					break;
				case MOVE_AND_TIE:
					sendToAll(writeMove(x, y, playerNum));
					endGame(MinigameResult.TIE, positionOf(p));
					break;
				case OVERLINE:
					p.getClient().getSession().send(writeForbiddenMove((byte) 1));
					break;
				case DOUBLE_THREES:
					p.getClient().getSession().send(writeForbiddenMove((byte) 2));
					break;
			}
		}

		public void redo(boolean agree, byte pos) {
			byte opponentPos = (byte) (pos == 0 ? 1 : 0);
			if (agree) {
				byte amountToRemove = (byte) (pos == 0 ^ currentPos == 0 ? 2 : 1);
				board[lastMove[0]][lastMove[1]] = 0;
				if (amountToRemove == 2)
					board[lastLastMove[0]][lastLastMove[1]] = 0;
				sendToAll(writeRedoAccept(amountToRemove, opponentPos));
			} else {
				getPlayerByPosition(opponentPos).getClient().getSession().send(writeRedoDecline());
			}
		}

		@Override
		public MiniroomType getMiniroomType() {
			return MiniroomType.OMOK;
		}

		@Override
		protected void reset() {
			for (int i = 0; i < COLUMNS; i++)
				for (int j = 0; j < ROWS; j++)
					board[i][j] = 0;
		}

		@Override
		protected byte[] getStartMessage() {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_START);
			lew.writeBool(ownerMovesFirst);
			return lew.getBytes();
		}

		private static byte[] writeRedoDecline() {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_ANSWER_REDO);
			lew.writeBool(false);
			return lew.getBytes();
		}

		private static byte[] writeRedoAccept(byte amountToRemove, byte pos) {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_ANSWER_REDO);
			lew.writeBool(true);
			lew.writeByte(amountToRemove);
			lew.writeByte(pos);
			return lew.getBytes();
		}

		private static byte[] writeMove(int x, int y, byte playerNum) {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_MOVE_OMOK);
			lew.writeInt(x);
			lew.writeInt(y);
			lew.writeByte(playerNum);
			return lew.getBytes();
		}

		private static byte[] writeForbiddenMove(byte reason) {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_CANNOT_MOVE);
			lew.writeByte(reason);
			return lew.getBytes();
		}
	}

	public static class MatchCards extends Minigame {
		private static final byte
			MOVE_RESULT_OWNER_UN_TURN = 0,
			MOVE_RESULT_VISITOR_UN_TURN = 1,
			MOVE_RESULT_OWNER_MATCH = 2,
			MOVE_RESULT_VISITOR_MATCH = 3
		;

		private int[] cards;
		private byte firstSelect;
		private int[] matches;

		public MatchCards(GameCharacter owner, String text, String password, byte size) {
			super(owner, text, password, size);
			int matchesCount = 0;
			switch (size) {
				case 0:
					matchesCount = 6;
					break;
				case 1:
					matchesCount = 10;
					break;
				case 2:
					matchesCount = 15;
					break;
			}
			cards = new int[matchesCount * 2];
			for (int i = 0; i < matchesCount; i++)
				cards[i] = cards[i + matchesCount] = i;
			matches = new int[2];
		}

		private void randomizeCards() {
			Random r = Rng.getGenerator();
			for (int i = 0; i < cards.length; i++) {
				int j = r.nextInt(cards.length);
				int t = cards[i];
				cards[i] = cards[j];
				cards[j] = t;
			}
		}

		public void selectCard(GameCharacter p, byte turnNo, byte card) {
			if (turnNo == 1) {
				firstSelect = card;
				getPlayerByPosition((byte) (positionOf(p) == 0 ? 1 : 0)).getClient().getSession().send(writeFirstSelect(card));
			} else if (turnNo == 0) {
				byte pos = positionOf(p);
				currentPos = (byte) (pos == 0 ? 1 : 0);
				if (cards[card] != cards[firstSelect]) {
					sendToAll(writeSecondSelect(firstSelect, card, pos == 0 ? MOVE_RESULT_OWNER_UN_TURN : MOVE_RESULT_VISITOR_UN_TURN));
				} else {
					matches[pos]++;
					sendToAll(writeSecondSelect(firstSelect, card, pos == 0 ? MOVE_RESULT_OWNER_MATCH : MOVE_RESULT_VISITOR_MATCH));
					if (matches[0] + matches[1] == cards.length / 2)
						endGame((matches[0] != matches[1]) ? MinigameResult.WIN : MinigameResult.TIE, pos);
				}
			}
		}

		@Override
		public MiniroomType getMiniroomType() {
			return MiniroomType.MATCH_CARDS;
		}

		@Override
		protected void reset() {
			randomizeCards();
			matches[0] = matches[1] = 0;
		}

		@Override
		protected byte[] getStartMessage() {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5 + 4 * cards.length);
			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_START);
			lew.writeBool(ownerMovesFirst);
			lew.writeByte((byte) cards.length);
			for (int i = 0; i < cards.length; i++)
				lew.writeInt(cards[i]);
			return lew.getBytes();
		}

		private static byte[] writeFirstSelect(byte select) {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5);
			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_SELECT_CARD);
			lew.writeByte((byte) 1);
			lew.writeByte(select);
			return lew.getBytes();
		}

		private static byte[] writeSecondSelect(byte firstSelect, byte secondSelect, byte result) {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_SELECT_CARD);
			lew.writeByte((byte) 0);
			lew.writeByte(secondSelect);
			lew.writeByte(firstSelect);
			lew.writeByte(result);
			return lew.getBytes();
		}
	}

	//TODO: actually calculate real scores (no idea how Nexon calculates it)
	private static int getScore(int wins, int ties, int losses) {
		int score = 2000;
		if (wins + ties + losses > 0) {
			score += wins * 2;
			score -= losses * 2;
		}
		return score;
	}

	private static void writeMinigameScores(LittleEndianWriter lew, GameCharacter p, MiniroomType type) {
		int wins = p.getMinigamePoints(type, MinigameResult.WIN);
		int ties = p.getMinigamePoints(type, MinigameResult.TIE);
		int losses = p.getMinigamePoints(type, MinigameResult.LOSS);
		lew.writeInt(type.byteValue());
		lew.writeInt(wins);
		lew.writeInt(ties);
		lew.writeInt(losses);
		lew.writeInt(getScore(wins, ties, losses));
	}
}
