package zsy.cn;

import java.util.ArrayList;



/**
 * 
 * �﷨��������
 *
 */


class GrammerAnalysisTree {
	public class Node {
		int mSymbol; 
		String mValue;             //�ڵ��value
		ArrayList<Node> mSonList;  //���ӽڵ�����
		Node mFather;              //���ڵ�

		public Node() {
			mSonList = new ArrayList<Node>();
		}

		public Node(int symbol) {
			mSymbol = symbol;
			mSonList = new ArrayList<Node>();
		}

		public void addSon(int index, Node son) {
			son.mFather = this;
			mSonList.add(index, son);
		}

		public Node addSon(int index, int symbol) {
			Node son = new Node(symbol);
			addSon(index, son);
			return son;
		}

		public Node getFather() {
			return mFather;
		}
	}

	public Node mRoot;

	public GrammerAnalysisTree() {
		mRoot = new Node();
		mRoot.mFather = null;
	}
}