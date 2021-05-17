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
	int n = 240; //�ʏ�m�[�h�̐�
	int n_High = 20; //���s���e�[�V�����̍����m�[�h��
	int n_adjust = 10; //�����m�[�h��
	double k = 1;
	double delta = 0.5;
	double xi = 0.1;
	
	/**
	 * ���s���e�[�V�������i�[
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
			//���b�Z�[�W��`�����E�`�����t�@�C�i���C�Y����Ă��Ȃ� or�@�`�����X�^�[�g�ł��Ȃ��i!can�j�Ȃ�΁Areturn 
		}
		
		// Try first the messages that can be delivered to final recipient
		//�@�܂��ŏ��Ɉ���ɓ͂��邱�Ƃ̏o���郁�b�Z�[�W���`�������B
		if (exchangeDeliverableMessages() != null) {
			//�@�����ڑ����̃m�[�h���Ẵ��b�Z�[�W������ or �ڑ����̃m�[�h�����g���Ẵ��b�Z�[�W�������Ă���Ȃ�΁Areturn
			return; // started a transfer, don't try others (yet)
		}
		
		// then try any/all message to any/all connection		
		tryAllMessagesToFriends();// ���ׂẴ��b�Z�[�W�����ׂẴR�l�N�V�������̃m�[�h�ɑ���B�@���̕�����啝�ύX����B
		calReputationDown();
		 /*java docs�Q��
		   MessageRouter.sortByQueueMode(List).
		 	Sorts/shuffles the given list according to the current sending queuemode. 
		 	The list can contain either Message or Tuple objects. Other objects cause error. 
		 
		   tryMessagesToConnections(MessageList, ConnectionList)
		    Tries to send all given messages to all given connections. �i�����P�̂��ׂẴ��b�Z�[�W�������Q�̂��ׂẴm�[�h�Ɍ����đ���j
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
			friends.add(c.getToNode());//Friend�m�[�h�����X�g�Ɋi�[
		}
		
		List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
		this.sortByQueueMode(messages);
		
		List<Connection> HighReputation = new ArrayList<Connection>();
		for(Connection c : connections) {
			int Me_Address = c.getToNode().getAddress();
			int Partnner_Address = c.getFromNode().getAddress();
		
			//Reputation �̍����m�[�h�݂̂��i�[�������X�g���쐬
			if(Repu[Partnner_Address]>Repu[Me_Address]) {
				HighReputation.add(c);
			}
			//Reputation�@�̒l��������i�����ł̓}�C�i�X�ŕ\���j�Ȃ�A�܂蒲���m�[�h�Ȃ烊�X�g�Ɋi�[
			if(Repu[Partnner_Address]<0) {
				HighReputation.add(c);
			}
			calReputationUp(Partnner_Address,Me_Address);
		}

		return tryMessagesToConnections(messages, HighReputation);
	}
	
	
	
	/*ActiveRouter.java���̈ȉ��̃��\�b�h���Q�l
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
	 * �m�[�ha�ƃm�[�hb�̒ʐM���Ƀ��s���e�[�V�������X�V
	 * @param a �m�[�ha
	 * @param b�@�m�[�hb
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
	 * �e�m�[�h�̃��s���e�[�V��������莞�Ԃ��ƂɌ���������
	 */
	public void calReputationDown() {
		for(int i=0; i < n + n_High;i++) {
			Repu[i] = (1-xi)*Repu[i];		
		}
	}


}