/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
//import java.util.Arrays;

import core.Connection;
import core.Message;
import core.Settings;
import core.SimClock;
//import routing.util.RoutingInfo;
import core.DTNHost;

/**
 * RBDR message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class RBDRRouter extends ActiveRouter {
	List<DTNHost> friends = new ArrayList<DTNHost>(new HashSet<>());

	/**
	 *　ノード数．本当はファイル入力にしたいね．めんどいからこのままで．
	 */
//	static final int n = 240; 

	/**
	 * 自身のアドレス
	 */
	int myAddress;
	DTNHost myHost;

	/**
	 * レピュテーション
	 */
	double myRepu;
	double partnnerRepu;

	double time;
	static final double decreaceInterval = 1800;

	/**
	 * レピュテーション計算の際の比例定数．スケールに利用．
	 */

	static final double k = 0.03;
	static final double delta = 0;
	static final double eta = 0.1;

	/**
	 * 友人ノードに対するレピュテーションを格納．
	 * RepuToF[i][j]は，ノードiがノードjのために保管しているレピュテーションの値を表す．
	 * 初期値はすべて-1．
	 * ノードと出会った際，値が-1なら初めて出会ったと判定
	 * -1でなければ，出会ったことがあるので，相手のレピュテーションに値を追加．
	 */
//	double RepuToF[] = new double[n];

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public RBDRRouter(Settings s) {
		super(s);

	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected RBDRRouter(RBDRRouter r) {
		super(r);
	}

	@Override
	public void update() {
		super.update();
		time = Math.floor(SimClock.getTime());
		//System.out.println(time);

		if(time !=0 && time % decreaceInterval==0) {
			calReputationDown(myHost);
		}

		myHost = getHost();
		myAddress = myHost.getAddress();
		myRepu = myHost.getRepu();


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
		//tryALLMessagesToAllConnections(); すべてのメッセージをすべてのコネクション中のノードに送る。　この部分を大幅変更する。

		tryAllMessagesToHigherRepu();//すべてのメッセージをレピュテーションの高いノードに送る．
		
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
	public RBDRRouter replicate() {
		return new RBDRRouter(this);
	}

	/**友人リストのすべてのノードにすべてのメッセージを送る．
	 * 
	構造は，{@link ActiveRouter#tryAllMessagesToAllConnections() tryAllMessagesToAllConnections}を参考
	 * @return Connectionsにメッセージを送るメソッドを返す．
	 * この場合，Connectionsの引数に代入されているのは
	 * High ReputationのノードとのConnectionのリストである．
	 */
	public Connection tryAllMessagesToHigherRepu(){
		myHost = getHost();
		myAddress = myHost.getAddress();
		myRepu = myHost.getRepu();

		List<Connection> connections = getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}

		/*	フレンドは未実装
		for(Connection c : connections)  {
			friends.add(c.getToNode());//Friendノードをリストに格納
		}
		 */

		List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
		this.sortByQueueMode(messages);

		List<Connection> higherRepuNode = new ArrayList<Connection>();
		for(Connection c : connections) {
			DTNHost partnner = c.getToNode();
			int partnnerAddress = c.getToNode().getAddress();

			if(myAddress == partnnerAddress) {
				partnner = c.getFromNode();
				partnnerAddress = c.getFromNode().getAddress();
			}


			double partnnerRepu = partnner.getRepu();

			//相手と初めて出会ったのならば
			/*
			if(getRepuToF(Me_Address,Partnner_Address)==-1) {

			}
			 */

			//Reputation の高いノードのみを格納したリストを作成						
			if(partnnerRepu > myRepu) {
				/*
				System.out.println("---------------------------------------------------");
				System.out.println("partnnerAddrss myAddress:"+ partnnerAddress +" "+myAddress);
				System.out.println("partnnerRepu myRepu:"+ partnnerRepu +" "+myRepu);
				 */
				higherRepuNode.add(c);
			}
			//Reputation　の値が無限大（ここではマイナスで表現）なら、つまり調整ノードならリストに格納
			if(partnnerRepu<0) {
				higherRepuNode.add(c);
			}

		}

		return tryMessagesToConnections(messages, higherRepuNode);
	}


	/*
	public double getRepuToF(int a) {
		return RepuToF[a];
	}
	*/
	/*
	@Override
	protected void addToSendingConnections(Connection con) {
		this.sendingConnections.add(con);
	}
	 */	
	@Override
	public void changedConnection(Connection con) {
		if(time!=0) {
			DTNHost partnner = con.getToNode();
			int partnnerAddress = con.getToNode().getAddress();
			double partnnerRepu = partnner.getRepu();

			if(myAddress == partnnerAddress) {
				partnner = con.getFromNode();
				partnnerAddress = con.getFromNode().getAddress();
			}else {
				if(myRepu!=-1 || partnnerRepu!=-1)
					calReputationUp(myHost,partnner);
			}
		}
	}



	/**
	 * ノードaとノードbの通信時にレピュテーションを更新
	 * @param a ノードa
	 * @param b　ノードb
	 */
	public void calReputationUp(DTNHost i, DTNHost j) {
		double ri = i.getRepu();
		double rj = j.getRepu();
		/*
		System.out.println("---------------------------------------------------");		
		System.out.println("node"+i.getAddress()+"'s Reputation was: " + ri);
		System.out.println("node"+j.getAddress()+"'s Reputation was: " + rj);
		*/
		//}

		double N = k*ri*rj;
		if(N>100000) {
			N = 100000;
		}

		double pa = ri/(ri+rj);
		double pb = rj/(ri+rj);

		double riUp = round2((1-delta)*pa*N);
		double rjUp = round2((1-delta)*pb*N);

		//		if(i.getAddress()>=250 || j.getAddress()>=250) {
		/*
		System.out.println();		
		System.out.println("node"+i.getAddress()+"'s Reputation go up: " + riUp);
		System.out.println("node"+j.getAddress()+"'s Reputation go up: " + rjUp);
		*/
		//	}

		double myRepu =  round1(ri + riUp);
		double partnnerRepu = round1(rj +rjUp);
		i.setRepu(myRepu);
		j.setRepu(partnnerRepu);

		//if(i.getAddress()>=250 || j.getAddress()>=250) {
		/*
		System.out.println();
		System.out.println("node"+i.getAddress()+"'s Reputation Updated!: " + i.getRepu());
		System.out.println("node"+j.getAddress()+"'s Reputation Updated!: " + j.getRepu());
		*/
		//}
	}


	/**
	 * 各ノードのレピュテーションを一定時間ごとに減少させる
	 */
	public void calReputationDown(DTNHost nodei) {
		myRepu = nodei.getRepu();

		//レピュテーションが１以上なら減衰
		if(myRepu>1.0) {
			myRepu = (1-eta)*myRepu;	
			//	System.out.println("Reputation" + i + " is Donw");

			//減衰後の値が１以上ならセット
			if(myRepu > 1.0) {
				nodei.setRepu(myRepu);
			}else {
				nodei.setRepu(1);
			}
		}
	}

	public double round1(double num){
		double c =((double)Math.round(num*1000))/1000;
		return c;
	}

	public double round2(double num){
		double c =((double)Math.round(num*100000))/100000;
		return c;
	}
}