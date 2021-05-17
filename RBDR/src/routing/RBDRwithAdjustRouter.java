/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import core.Connection;
import core.Message;
import core.Settings;
import core.DTNHost;

/**
 * RBDR message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class RBDRwithAdjustRouter extends ActiveRouter {
	List<DTNHost> f;
	List<DTNHost> friends = new ArrayList<DTNHost>(new HashSet<>(f));
	int n = 240; //通常ノードの数
	int n_High = 20; //レピュテーションの高いノード数
	int n_adjust = 10; //調整ノード数
	double k = 1;
	double delta = 0.5;
	double xi = 0.1;
	
	/**
	 * レピュテーションを格納
	 */
	double Repu[] = new double[n + n_High + n_adjust];
	double RepuToF[][] = new double[n + n_High + n_adjust][n + n_High + n_adjust];
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public RBDRwithAdjustRouter(Settings s) {
		super(s);
		//TODO: read&use epidemic router specific settings (if any)
		
		for(int i=0 ; i<n; i++) {
			Repu[i] = 1;
		}
		for(int i = n; i<n+n_High;i++) {
			Repu[i] = 10;
		}
		for(int i= n+n_High; i<n + n_High + n_adjust; i++) {
			Repu[i] = -1;
		}
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected RBDRwithAdjustRouter(RBDRwithAdjustRouter r) {
		super(r);
		//TODO: copy epidemic settings here (if any)
	}
			
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
			//メッセージを伝送中・伝送がファイナライズされていない or　伝送がスタートできない（!can）ならば、return 
		}
		
		// Try first the messages that can be delivered to final recipient
		//　まず最初に宛先に届けることの出来るメッセージが伝送される。
		if (exchangeDeliverableMessages() != null) {
			//　もし接続中のノードあてのメッセージがある or 接続中のノードが自身あてのメッセージを持っているならば、return
			return; // started a transfer, don't try others (yet)
		}
		
		// then try any/all message to any/all connection		
		tryAllMessagesToFriends();// すべてのメッセージをすべてのコネクション中のノードに送る。　この部分を大幅変更する。
		calReputationDown();
		 /*java docs参照
		   MessageRouter.sortByQueueMode(List).
		 	Sorts/shuffles the given list according to the current sending queuemode. 
		 	The list can contain either Message or Tuple objects. Other objects cause error. 
		 
		   tryMessagesToConnections(MessageList, ConnectionList)
		    Tries to send all given messages to all given connections. （引数１のすべてのメッセージを引数２のすべてのノードに向けて送る）
		    Connections are first iterated in the order they are in the list and for everyconnection, 
		    the messages are tried in the order they are in the list.
		    Once an accepting connection is found, no other connections or messages are tried.*/
		}

	@Override
	public RBDRwithAdjustRouter replicate() {
		return new RBDRwithAdjustRouter(this);
	}
	
	public Connection tryAllMessagesToFriends(){
		List<Connection> connections = getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}
		
		for(Connection c : connections)  {
			friends.add(c.getToNode());//Friendノードをリストに格納
		}
		
		List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
		this.sortByQueueMode(messages);
		
		List<Connection> HighReputation = new ArrayList<Connection>();
		for(Connection c : connections) {
			int Me_Address = c.getToNode().getAddress();
			int Partnner_Address = c.getFromNode().getAddress();
		
			//Reputation の高いノードのみを格納したリストを作成
			if(Repu[Partnner_Address]>Repu[Me_Address]) {
				HighReputation.add(c);
			}
			//Reputation　の値が無限大（ここではマイナスで表現）なら、つまり調整ノードならリストに格納
			if(Repu[Partnner_Address]<0) {
				HighReputation.add(c);
			}
			calReputationUp(Partnner_Address,Me_Address);
		}

		return tryMessagesToConnections(messages, HighReputation);
	}
	
	
	
	/*ActiveRouter.java内の以下のメソッドを参考
	protected Connection tryAllMessagesToAllConnections(){
		List<Connection> connections = getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}

		List<Message> messages = 
			new ArrayList<Message>(this.getMessageCollection());
		this.sortByQueueMode(messages);

		return tryMessagesToConnections(messages, connections);
	}
	*/

	/**
	 * ノードaとノードbの通信時にレピュテーションを更新
	 * @param a ノードa
	 * @param b　ノードb
	 */
	public void calReputationUp(int a , int b) {
		double Ra;
		double Rb;

		double N = k*Repu[a]*Repu[b];
		double pa = Repu[a]/(Repu[a]+Repu[b]);
		double pb = Repu[b]/(Repu[a]+Repu[b]);
		
		Ra = pa*N;
		Rb = pb*N;
		
		Repu[a] += (1-delta)*Ra;
		Repu[b] += (1-delta)*Rb;
	}
	
	/**
	 * 各ノードのレピュテーションを一定時間ごとに減少させる
	 */
	public void calReputationDown() {
		for(int i=0; i < n + n_High;i++) {
			Repu[i] = (1-xi)*Repu[i];		
		}
	}


}