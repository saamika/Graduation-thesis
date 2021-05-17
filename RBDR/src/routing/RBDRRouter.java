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
	 *�@�m�[�h���D�{���̓t�@�C�����͂ɂ������ˁD�߂�ǂ����炱�̂܂܂ŁD
	 */
//	static final int n = 240; 

	/**
	 * ���g�̃A�h���X
	 */
	int myAddress;
	DTNHost myHost;

	/**
	 * ���s���e�[�V����
	 */
	double myRepu;
	double partnnerRepu;

	double time;
	static final double decreaceInterval = 1800;

	/**
	 * ���s���e�[�V�����v�Z�̍ۂ̔��萔�D�X�P�[���ɗ��p�D
	 */

	static final double k = 0.03;
	static final double delta = 0;
	static final double eta = 0.1;

	/**
	 * �F�l�m�[�h�ɑ΂��郌�s���e�[�V�������i�[�D
	 * RepuToF[i][j]�́C�m�[�hi���m�[�hj�̂��߂ɕۊǂ��Ă��郌�s���e�[�V�����̒l��\���D
	 * �����l�͂��ׂ�-1�D
	 * �m�[�h�Əo������ہC�l��-1�Ȃ珉�߂ďo������Ɣ���
	 * -1�łȂ���΁C�o��������Ƃ�����̂ŁC����̃��s���e�[�V�����ɒl��ǉ��D
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
			//���b�Z�[�W��`�����E�`�����t�@�C�i���C�Y����Ă��Ȃ� or�@�`�����X�^�[�g�ł��Ȃ��i!can�j�Ȃ�΁Areturn 
		}

		// Try first the messages that can be delivered to final recipient
		//�@�܂��ŏ��Ɉ���ɓ͂��邱�Ƃ̏o���郁�b�Z�[�W���`�������B
		if (exchangeDeliverableMessages() != null) {
			//�@�����ڑ����̃m�[�h���Ẵ��b�Z�[�W������ or �ڑ����̃m�[�h�����g���Ẵ��b�Z�[�W�������Ă���Ȃ�΁Areturn
			return; // started a transfer, don't try others (yet)
		}

		// then try any/all message to any/all connection		
		//tryALLMessagesToAllConnections(); ���ׂẴ��b�Z�[�W�����ׂẴR�l�N�V�������̃m�[�h�ɑ���B�@���̕�����啝�ύX����B

		tryAllMessagesToHigherRepu();//���ׂẴ��b�Z�[�W�����s���e�[�V�����̍����m�[�h�ɑ���D
		
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
	public RBDRRouter replicate() {
		return new RBDRRouter(this);
	}

	/**�F�l���X�g�̂��ׂẴm�[�h�ɂ��ׂẴ��b�Z�[�W�𑗂�D
	 * 
	�\���́C{@link ActiveRouter#tryAllMessagesToAllConnections() tryAllMessagesToAllConnections}���Q�l
	 * @return Connections�Ƀ��b�Z�[�W�𑗂郁�\�b�h��Ԃ��D
	 * ���̏ꍇ�CConnections�̈����ɑ������Ă���̂�
	 * High Reputation�̃m�[�h�Ƃ�Connection�̃��X�g�ł���D
	 */
	public Connection tryAllMessagesToHigherRepu(){
		myHost = getHost();
		myAddress = myHost.getAddress();
		myRepu = myHost.getRepu();

		List<Connection> connections = getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}

		/*	�t�����h�͖�����
		for(Connection c : connections)  {
			friends.add(c.getToNode());//Friend�m�[�h�����X�g�Ɋi�[
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

			//����Ə��߂ďo������̂Ȃ��
			/*
			if(getRepuToF(Me_Address,Partnner_Address)==-1) {

			}
			 */

			//Reputation �̍����m�[�h�݂̂��i�[�������X�g���쐬						
			if(partnnerRepu > myRepu) {
				/*
				System.out.println("---------------------------------------------------");
				System.out.println("partnnerAddrss myAddress:"+ partnnerAddress +" "+myAddress);
				System.out.println("partnnerRepu myRepu:"+ partnnerRepu +" "+myRepu);
				 */
				higherRepuNode.add(c);
			}
			//Reputation�@�̒l��������i�����ł̓}�C�i�X�ŕ\���j�Ȃ�A�܂蒲���m�[�h�Ȃ烊�X�g�Ɋi�[
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
	 * �m�[�ha�ƃm�[�hb�̒ʐM���Ƀ��s���e�[�V�������X�V
	 * @param a �m�[�ha
	 * @param b�@�m�[�hb
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
	 * �e�m�[�h�̃��s���e�[�V��������莞�Ԃ��ƂɌ���������
	 */
	public void calReputationDown(DTNHost nodei) {
		myRepu = nodei.getRepu();

		//���s���e�[�V�������P�ȏ�Ȃ猸��
		if(myRepu>1.0) {
			myRepu = (1-eta)*myRepu;	
			//	System.out.println("Reputation" + i + " is Donw");

			//������̒l���P�ȏ�Ȃ�Z�b�g
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