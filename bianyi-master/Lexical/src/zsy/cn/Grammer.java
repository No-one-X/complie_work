package zsy.cn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Stack;

/**
 * �ķ�������
 */
class Grammer {
	private int mTerminalsNums; // �ս���ĸ���
	private int mNonTerminalsNums; // ���ս���ĸ���
	private int mVocabularyNums; // ���ʱ�ĸ��� = �ս������ + ���ս������
	private int mStartSymbol; // ��ʼ����
	private int mLambda; // �յı��
	private ArrayList<ArrayList<Prod>> mProductions; // ����ʽ�б�
	private HashSet<Integer>[] mFirstSet; // First��
	private HashSet<Integer>[] mFollowSet; // Follow��
	private int[][] mPredictMap; // Ԥ�������
	private ArrayList<CompileError> mParserErrorList; // �﷨��������
	private HashMap<String, Integer> mTerminalsMap; // �ս�����ƺͱ�Ŷ��ձ�
	private HashMap<String, Integer> mNonTerminalsMap; // ���ս�����ƺͱ�Ŷ��ձ�
	private String[] mTerminals; // ��Ŵ�С�����ս������
	private ArrayList<String> mNonTerminals; // ��Ŵ�С������ս������
	private GrammerAnalysisTree mGrammerTree; // �﷨��
	private static final int PREDICT_NULL = -1; // Ԥ��������ֵ�Ķ���
	private static final int PREDICT_SYNCH = -2; // Ԥ�������ͬ���Ǻ�

	public Grammer(String terminalsDictFileName,
			String originalGrammerFileName, String newGrammerFileName,
			String predictMapFileName) {
		// ����ԭʼ�ķ�
		initFromFile(terminalsDictFileName, originalGrammerFileName);
		// ��ȡֱ�ӹ���ǰ׺
		factor();
		// ɾ��ֱ����ݹ�
		removeLeftRecursion();
		// ������ӹ���ǰ׺������q_�ķ�
		removeIndirectFactor();
		// ���ļ��������������ķ�
		printProductionList(newGrammerFileName, false, true);
		// ����first��
		fillFirstSet();
		// �������з��ս����follow��
		fillFollowSet();
		// ����Ԥ�������
		createPredictMap();
		// ���ļ������Ԥ�������
		printPredictMap(predictMapFileName, true, true, true, true);

		mParserErrorList = new ArrayList<CompileError>();

	}

	/**
	 * ɾ���ȼ۵��ķ������ս��
	 */
	private void removeSameProds() {
		boolean changes;
		do {
			changes = false;
			for (int i = mProductions.size() - 1; i >= 0; i--) {  //���ﴦ�����һ������ʽ�����е���ͬԪ��
				ArrayList<Prod> prodsA = mProductions.get(i);
				for (int j = prodsA.size() - 1; j >= 0; j--) {
					Prod pA = prodsA.get(j);
					for (int k = j - 1; k >= 0; k--) {
						if (pA.equals(prodsA.get(k))) { //�������ͬ����������ʽ��ɾ��һ��
							prodsA.remove(j);
							break;
						}
					}
				}
			}
			for (int i = 0; i < mProductions.size(); i++) {  //���ﴦ����ǿ��ܳ��ֵ�������ȫ��ͬ�Ĳ���ʽ����
				ArrayList<Prod> prodsA = mProductions.get(i);
				for (int j = i + 1; j < mProductions.size(); j++) {
					ArrayList<Prod> prodsB = mProductions.get(j);
					if (prodsA.size() == prodsB.size()) {
						boolean match = true;
						for (int k = prodsA.size() - 1; k >= 0 && match; k--) {
							boolean matchOne = false;
							for (int m = prodsB.size() - 1; m >= 0 && !matchOne; m--) {
								if (prodsA.get(k).equals(prodsB.get(m))) {  //���������С��ȵĲ���ʽ�Ƿ�ÿһ��Ԫ�ش�СҲ��ͬ
									matchOne = true;
								}
							}
							if (!matchOne) {
								match = false;
							}
						}
						if (match) {   //�����Ĵ���������������ʽ����
							changes = true;
							mProductions.remove(j);//ɾ������֮һ
							mNonTerminalsNums--;   //�ܷ��ս��������һ����Ϊֻ�з��ս�����ܲ�������ʽ
							for (int k = mProductions.size() - 1; k >= 0; k--) {
								ArrayList<Prod> prodsC = mProductions.get(k);
								for (int m = prodsC.size() - 1; m >= 0; m--) {
									Prod pC = prodsC.get(m);
									if (pC.mLhs > j + mTerminalsNums) { //ԭ����ɾ������ʽ����Ĳ���ʽ���Ҫ��Ӧ����ǰ�ƶ�һλ�����󲿼�һ
										pC.mLhs--;
									} else if (pC.mLhs == j + mTerminalsNums) {
										pC.mLhs = i + mTerminalsNums;
									}
									for (int t = pC.mRhs.size() - 1; t >= 0; t--) {
										int pcr = pC.mRhs.get(t);    //�Բ���ʽ�Ҳ�Ԫ�صĲ�����֮ǰ����
										if (pcr == j + mTerminalsNums) {
											pcr = i + mTerminalsNums;
											pC.mRhs.set(t, pcr);
										} else if (pcr > j + mTerminalsNums) {
											pcr--;
											pC.mRhs.set(t, pcr);
										}
									}
								}
							}
							j--;
						}

					}
				}
			}
		} while (changes);
	}

	/**
	 * ���ļ���������еĲ���ʽ
	 * 
	 * @param outputFileName
	 *            �ļ���
	 * @param printNumber
	 *            �Ƿ����������ʽ
	 * @param printName
	 *            �Ƿ����������ʽ
	 */
	private void printProductionList(String outputFileName,
			boolean printNumber, boolean printName) {
		try {
			FileOutputStream fout = new FileOutputStream(outputFileName);

			int tot = 0;
			for (int i = mProductions.size() - 1; i >= 0; i--) {
				tot += mProductions.get(i).size();
			}
			fout.write((tot + "\r\n").getBytes());
			for (int i = 0, len = mProductions.size(); i < len; i++) {
				ArrayList<Prod> prods = mProductions.get(i);
				for (int j = 0; j < prods.size(); j++) {
					Prod p = prods.get(j);
					printProduction(fout, p, printNumber, printName);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * ���ļ���fout���������ʽp
	 * 
	 * @param fout
	 *            Ҫ��������ļ���
	 * @param p
	 *            ����ʽp
	 * @param printNumber
	 *            �Ƿ�������ʽ��ӡ
	 * @param printName
	 *            �Ƿ�������ʽ��ӡ
	 * @throws IOException
	 */
	private void printProduction(FileOutputStream fout, Prod p,
			boolean printNumber, boolean printName) throws IOException {
		if (printNumber) {
			fout.write((p.mLhs + "\t->\t").getBytes());      //���ļ����������ʽ����
			for (int j = 0; j < p.mRhs.size(); j++) {        //���ļ��������Ӧ����ʽ���Ҳ�
				fout.write((" " + p.mRhs.get(j)).getBytes());
			}
			fout.write("\r\n".getBytes());
		}
		if (printName) {
			fout.write((getWordName(p.mLhs) + "\t->\t").getBytes());   //���ļ����������ʽ����
			for (int j = 0; j < p.mRhs.size(); j++) {
				fout.write((getWordName(p.mRhs.get(j))).getBytes());   //���ļ��������Ӧ����ʽ���Ҳ�
			}
			fout.write("\r\n".getBytes());
		}
	}


	/**
	 * ��ȡ�ս�����߷��ս��i������
	 * 
	 * @param i
	 * @return
	 */
	private String getWordName(int i) {
		if (i >= 0 && i < mTerminalsNums) {
			return "[" + mTerminals[i] + "]";
		} else if (i >= mTerminalsNums
				&& i < mTerminalsNums + mNonTerminals.size()) {
			return "<" + mNonTerminals.get(i - mTerminalsNums) + ">";
		}
		return "<" + i + ">";
	}

	/**
	 * ���ļ�������﷨��
	 * 
	 * @param terminalsDictFileName
	 * @param grammerFileName
	 */
	private void initFromFile(String terminalsDictFileName,
			String grammerFileName) {
		try {
			Scanner terCin = new Scanner(new File(terminalsDictFileName));
			mTerminalsNums = terCin.nextInt(); //�ʷ�����һ���ж����У��洢���ļ��ĵ�һ����
			mTerminalsMap = new HashMap<String, Integer>();// �ս�����ƺͱ�Ŷ��ձ�
			mTerminals = new String[mTerminalsNums];// ��Ŵ�С�����ս������
			for (int i = mTerminalsNums; i > 0; i--) {//�����ս�����ƺͱ�Ŷ��ձ�
				terCin.next();
				int num = terCin.nextInt();
				String name = terCin.next();
				terCin.nextLine();
				mTerminalsMap.put(name, num);
				mTerminals[num] = name;
			}
			mLambda = 0;
			mStartSymbol = mTerminalsNums;

			mNonTerminalsMap = new HashMap<String, Integer>();// ���ս�����ƺͱ�Ŷ��ձ�
			mNonTerminals = new ArrayList<String>();// �ս�����ƺͱ�Ŷ��ձ�
			mNonTerminalsNums = 0;
			Scanner cin = new Scanner(new File(grammerFileName));
			int prodN = cin.nextInt();    //���ʽһ���ж����֣�����д���ļ��ĵ�һ������
			cin.nextLine();
			mProductions = new ArrayList<ArrayList<Prod>>();//����ʽ�б�
			for (int i = 0; i < prodN; i++) {
				Prod p = new Prod();
				p.mRhs = new ArrayList<Integer>();

				String sProduction = cin.nextLine(); //ÿ�о���һ�ֲ���ʽ
				String sProductionTail = removeNSPHeader(sProduction);//[]�ڵ����ս����<>�ڵ��Ƿ��н��ַ���
                //����ֻҪ<>����[]�е����ݣ�����Ҫ������ȥ��
				String sLeftWord = getSingleWord(sProductionTail);//�õ�ȥ��'['��'<'�ķ��ս�����ս����
				if (sLeftWord == null || sLeftWord.charAt(0) != '<') {//�����쳣�󱨴�
					System.out.println("���ص�" + i + "���ķ�����" + sProduction);
					System.out.println("����ԭ����߲��ԣ�" + sProductionTail + "$"
							+ sLeftWord);
					continue;//ֱ�ӿ�ʼ��һ��ѭ��
				}
				String leftWord = sLeftWord.substring(1);//ȥ��ǰ���'<'����']'���õ�����ķ��ս��(�ս��)
				//System.out.println(leftWord);
				Integer leftIndex = mNonTerminalsMap.get(leftWord);//�ӷ��ս���еõ����н��ַ��ı��
				ArrayList<Prod> currentProdList;
				if (leftIndex == null) {//���粻���ڵĻ�
					leftIndex = mTerminalsNums + mNonTerminalsNums++; //����һ�����
					mNonTerminals.add(leftWord);              //�ֱ������ս����Ͷ��ձ���
					mNonTerminalsMap.put(leftWord, leftIndex);
					currentProdList = new ArrayList<Prod>();
					mProductions.add(currentProdList);//�������ʽ��������
				} else {//������ڵĻ�
					currentProdList = mProductions.get(leftIndex
							- mTerminalsNums);       //�Ӳ���ʽ�б��еõ�����ʽ
				}
				p.mLhs = leftIndex;

				sProductionTail = removeNSPHeader(sProductionTail
						.substring(sLeftWord.length()));      //�õ�����ʽ�ĺ��Σ���A->B|C|D��B��C��D����
				String sRightWord, rightWord;
				Integer rightIndex;
				while ((sRightWord = getSingleWord(sProductionTail)) != null) {
					rightWord = sRightWord.substring(1);
					if (sRightWord.charAt(0) == '<') {       //��'<'��ͷ˵���Ƿ��ս��
						rightIndex = mNonTerminalsMap.get(rightWord);//�ӷ��н�map���ҵ���Ӧ�ı��
						if (rightIndex == null) {//û�ҵ�
							rightIndex = mTerminalsNums + mNonTerminalsNums++;
							mNonTerminals.add(rightWord);
							mNonTerminalsMap.put(rightWord, rightIndex);
							mProductions.add(new ArrayList<Prod>());
						}
					} else if (sRightWord.charAt(0) == '[') {//�������ս���Ļ�
						rightIndex = mTerminalsMap.get(rightWord);
					} else {
						rightIndex = null;
					}
					if (rightIndex == null) {
						break;
					}
					p.mRhs.add(rightIndex);
					sProductionTail = removeNSPHeader(sProductionTail
							.substring(sRightWord.length()));
				}
				if (sRightWord != null) {
					System.out.println("���ص�" + i + "���ķ�����" + sProduction);
					continue;
				}
				p.mComment = sProductionTail.replace("\t", "");
				currentProdList.add(p);
				//System.out.println(currentProdList.toString());
				//System.out.println(mProductions.toString());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ȥ���ַ���s��'<'��'['ǰ���ַ�
	 * 
	 * @param s
	 * @return
	 */
	private static String removeNSPHeader(String s) {
		if (s == null)
			return null;
		int i = s.indexOf('<');
		int j = s.indexOf('[');
		if (i < j) {
			if (i > -1) {
				return s.substring(i);
			}
			return s.substring(j);
		} else if (i > j) {
			if (j > -1) {
				return s.substring(j);
			}
			return s.substring(i);
		}
		return s;
	}

	/**
	 * ���ַ���s�л�ȡ��һ���ս�����߷��ս��
	 * 
	 * @param s
	 * @return
	 */
	private static String getSingleWord(String s) {
		if (s == null)
			return null;
		int i;
		if (s.charAt(0) == '<') {
			i = s.indexOf('>', 2);// ���ַ���2��λ�ÿ�ʼ����'>'���ţ���������λ��
		} else if (s.charAt(0) == '[') {
			i = s.indexOf(']', 2);
		} else {
			return null;
		}
		if (i < 0)
			return null;
	//System.out.println(s.substring(0, i));
		return s.substring(0, i);
	}

	/**
	 * ����First(apha)
	 * 
	 * @param hashSet
	 *            ������ļ�����ӵ�hashSet��
	 * @param alpha
	 *            Ҫ����ı��ʽ //������ʽ���Ҳ�������һ����������
	 */
	private void computeFirst(HashSet<Integer> hashSet, ArrayList<Integer> alpha) {
		if (alpha.size() == 0) {
			hashSet.add(mLambda);  //mLambda��С��ǰ�汻��ֵΪ0
		} else {
			boolean oriContainLambda = hashSet.contains(mLambda); //�����hashset�а����գ��򷵻�true
			hashSet.addAll(mFirstSet[alpha.get(0)]);   //�õ��Ҳ��ĵ�һ��Ԫ��
			int i = 0;
			for (i = 1; i < alpha.size()
					&& mFirstSet[alpha.get(i - 1)].contains(mLambda); i++) {
				hashSet.addAll(mFirstSet[alpha.get(i)]); //�����Ҳ���ǰi��Ԫ�ض�Ϊ�գ���ô�ͽ���i��Ԫ�ؼ���
			}
			if (!oriContainLambda) {    //����û�п�Ԫ��
				if (i < alpha.size()
						|| !mFirstSet[alpha.get(i - 1)].contains(mLambda)) {
					hashSet.remove(mLambda); //������Ԫ�ض��ǿգ��ͽ��մ�hashset���Ƴ�
				}
			}
		}
	}

	/**
	 * ����First��
	 */
	private void fillFirstSet() {
		mVocabularyNums = mNonTerminalsNums + mTerminalsNums;
		mFirstSet = new HashSet[mVocabularyNums]; //��СΪ�ս���ӷ��ս��֮��
		for (int i = mFirstSet.length - 1; i >= 0; i--) {//
			mFirstSet[i] = new HashSet<Integer>();
		}
		for (int i = mTerminalsNums - 1; i >= 0; i--) { //0-84����ս������ʷ������ֵ��Ӧ
			mFirstSet[i].add(i);                        //�ս���ŵ�first���Ͼ�������
			//System.out.println(mFirstSet[i].toString());
		}
		boolean changes;
		do {
			changes = false;
			for (int k = mProductions.size() - 1; k >= 0; k--) {
				ArrayList<Prod> currentProds = mProductions.get(k); //�õ�һ������ʽ����
				for (int i = currentProds.size() - 1; i >= 0; i--) {
					Prod p = currentProds.get(i);                //������ȡ��һ������ʽ
					int oriNum = mFirstSet[p.mLhs].size();       //�鿴��first�����Ƿ���Ԫ�أ���ʼʱ��СΪ0
					//System.out.println(oriNum);
					computeFirst(mFirstSet[p.mLhs], p.mRhs);
					int newNum = mFirstSet[p.mLhs].size();       //�����µ�first���ϲ��ǿ���
					changes = changes || oriNum != newNum;       //change��ֵ��Υtrue������������do����whileѭ��
				}
			}
		} while (changes);
	}

	/**
	 * �������з��ս����Follow��
	 */
	private void fillFollowSet() {
		mFollowSet = new HashSet[mNonTerminalsNums];           //follow����С�϶�Ϊ���ս���ŵĽ��
		for (int i = mFollowSet.length - 1; i >= 0; i--) {
			mFollowSet[i] = new HashSet<Integer>();            //ÿ�����ս���Ŷ�Ӧһ��follow����
		}
		//System.out.println(mStartSymbol - mTerminalsNums);
		mFollowSet[mStartSymbol - mTerminalsNums].add(mLambda);//mStartSymbol - mTerminalsNums��С��ʵ����0��mStartSymbol֮ǰ����ֵΪmTerminalsNums
		boolean changes;
		int terminalsLength = mTerminalsNums;
		HashSet<Integer> hashSet = new HashSet<Integer>();

		do {
			changes = false;
			for (int k = mProductions.size() - 1; k >= 0; k--) {
				ArrayList<Prod> currentProds = mProductions.get(k);
				for (int i = currentProds.size() - 1; i >= 0; i--) {
					Prod p = currentProds.get(i);
					int len = p.mRhs.size();//len��p�Ҳ��Ĵ�С
					int A = p.mLhs - terminalsLength; // A�Ĵ�СΪ�󲿵���ֵ-�ս���Ķ���
					ArrayList<Integer> alpha = (ArrayList<Integer>) p.mRhs
							.clone();    //alpha����p���Ҳ�����
					for (int j = 0; j < len; j++) {
						int B = alpha.get(0);//�õ��Ҳ��ĵ�һ��Ԫ��
						alpha.remove(0);    //��B�������ǰ���Ҳ�������Ϊ����ʣ��Ԫ�ص�first���ϣ��൱����B��follow����
						if (B >= terminalsLength) {   //����B�����ս��,��Ϊ�ս������0-terminalsLength֮��
							B -= terminalsLength;     // B�Ĵ�СΪ�Ҳ���һ��Ԫ�ص���ֵ-�ս���Ķ��٣�����Ϊ�˴����µ�follow�����У�follow���ϵĴ�Сֻ�з��ս��ַ������Ĵ�С
							int oriN = mFollowSet[B].size(); //�տ�ʼʱB��follow���ϴ�СΪ0
							hashSet.clear();
							computeFirst(hashSet, alpha);    //��ʣ��Ԫ�ص�first����
							if (hashSet.contains(mLambda)) { //������ʣ�µ�first����Ϊ�գ������ж���follow���ϵĵ��������,����B�����Ѿ�û��Ԫ���ˣ���ô�ͽ�B��follow���ϼ���A��follow������
								mFollowSet[B].addAll(mFollowSet[A]);
								hashSet.remove(mLambda);    //ȥ����
							}
							mFollowSet[B].addAll(hashSet);
							int nowN = mFollowSet[B].size();
							changes = changes || oriN < nowN;
							/**
							 * ��ӡһ���� if (B == 10) { printProduction(p, true,
							 * true); for (int ti = 0; ti < mTerminalsNums;
							 * ti++) { if (mFollowSet[B].contains(ti)) {
							 * System.out.print(getWordName(ti)); } }
							 * System.out.println(); }
							 */
						}
					}
				}
			}
		} while (changes);
	}

	/**
	 * ��ȡ����ǰ׺
	 */
	private void factor() {
		boolean found;
		ArrayList<Prod> sameHeaderList = new ArrayList<Prod>();
		do {
			found = false;
			for (int g = mProductions.size() - 1; g >= 0; g--) {
				ArrayList<Prod> currentProds = mProductions.get(g);
				for (int i = currentProds.size() - 1; i > 0; i--) {
					Prod pA = currentProds.get(i);
					int sameLen = pA.mRhs.size();  //����ʽ�Ҳ��ж��ٸ��ս��/���ս��
					if (pA.mRhs.get(0) == mLambda) { //�������ʽ�Ҳ�Ϊ�գ���ֱ�ӽ�����һ��ѭ��
						continue;
					}
					sameHeaderList.clear();       //ÿ�ζ�Ҫ���
					sameHeaderList.add(pA);       //�ȼ���pA
					for (int j = i - 1; j >= 0; j--) {
						Prod pB = currentProds.get(j);
						int pBLen = pB.mRhs.size();//����ʽ�Ҳ��ж��ٸ��ս��/���ս��
						int k;
						for (k = 0; k < sameLen && k < pBLen
								&& pA.mRhs.get(k).equals(pB.mRhs.get(k)); k++) //Ѱ���ǰ׺
							;
						if (k > 0) {
							sameLen = k;           //�����ǰ׺�ĳ���Ϊk
							sameHeaderList.add(pB);//��pB����,��˵��pB��pA�й���ǰ׺
						}
					}
					if (sameHeaderList.size() > 1) { //����Ѿ���һЩprod����ǰ׺��ͬ
						/**
						 * ������û��ֱ�ӿ��õ�
						 */
						boolean foundUsable = false;
						for (int ti = mProductions.size() - 1; ti >= 0
								&& !foundUsable; ti--) {
							ArrayList<Prod> mayBeList = mProductions.get(ti);
							if (mayBeList.size() == sameHeaderList.size()) { //���ֱ�ӿ��ã���һ�㣺����ʽ���ϵĴ�С������ͬ
								boolean match = true;
								for (int tj = mayBeList.size() - 1; tj >= 0
										&& match; tj--) {
									Prod mayBeProd = mayBeList.get(tj);
									boolean matchOne = false;
									for (int tk = sameHeaderList.size() - 1; tk >= 0
											&& !matchOne; tk--) {
										Prod curProd = sameHeaderList.get(tk);
										if (mayBeProd.mRhs.size() == curProd.mRhs
												.size() - sameLen) {  //���ֱ�ӿ��ã��ڶ��㣺����ʽ�Ҳ��Ĵ�С��������еĲ���ʽ�Ҳ�ȥ������ǰ׺��Ĵ�С���
											boolean matchEach = true;
											for (int tp = mayBeProd.mRhs.size() - 1; tp >= 0
													&& matchEach; tp--) {
												if (!mayBeProd.mRhs
														.get(tp)
														.equals(curProd.mRhs
																.get(tp
																		+ sameLen))) {
													matchEach = false;
												}
											}
											if (matchEach) { //���ֱ�ӿ���,�����㣺����ʽ�Ҳ�������Ԫ�ر������
												matchOne = true;
											}
										}
									}
									if (!matchOne) {
										match = false;
									}
								}
								if (match) {
									foundUsable = true;
									for (int tp = sameHeaderList.size() - 1; tp >= 0; tp--) {
										currentProds.remove(sameHeaderList
												.get(tp));
									}
									Prod pC = new Prod(); //�½�һ�����ʽpC
									pC.mLhs = pA.mLhs;    //ʹ����pA������ͬ
									pC.mRhs = getSubList(pA.mRhs, 0, sameLen); //��pA���Ҳ��Ĺ���������ȡ��������pC���Ҳ�
									pC.mRhs.add(ti + mTerminalsNums);  //ֱ�ӽ��Ҳ�ʣ�µĲ��ּ����ҵ���Ԫ��
									currentProds.add(pC);
									//System.out.println(currentProds.toString());
								}
							}
						}
						if (foundUsable) {
							found = true;
							break;
						}
						/**
						 * û�������
						 */
						Prod pC = new Prod();
						pC.mLhs = pA.mLhs;
						pC.mRhs = getSubList(pA.mRhs, 0, sameLen);
						ArrayList<Prod> newProd = new ArrayList<Prod>();
						for (int j = sameHeaderList.size() - 1; j >= 0; j--) {
							Prod pN = sameHeaderList.get(j);
							currentProds.remove(pN);
							pN.mLhs = mTerminalsNums + mNonTerminalsNums;
							pN.mRhs = getSubList(pN.mRhs, sameLen,
									pN.mRhs.size());
							if (pN.mRhs.size() == 0) {
								pN.mRhs.add(mLambda);
							}
							newProd.add(pN);
						}
						pC.mRhs.add(mTerminalsNums + mNonTerminalsNums++);
						currentProds.add(pC);
						mProductions.add(newProd);
						found = true;
						break;
					}
				}
			}
		} while (found);
	}

	/**
	 * ��ȡa��[startIndex, endIndex)���Ԫ��
	 * 
	 * @param a
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	private <T> ArrayList<T> getSubList(ArrayList<T> a, int startIndex,
			int endIndex) {
		ArrayList<T> newArrayList = new ArrayList<T>();
		int len = a.size();
		for (int i = startIndex; i < len && i < endIndex; i++) {
			newArrayList.add(a.get(i));
		}
		return newArrayList;
	}

	/**
	 * ɾ����ݹ�
	 */
	private void removeLeftRecursion() {
		boolean found;
		do {
			found = false;
			for (int k = mProductions.size() - 1; k >= 0; k--) {
				ArrayList<Prod> currentProds = mProductions.get(k);
				for (int i = currentProds.size() - 1; i >= 0; i--) {
					Prod p = currentProds.get(i);
					if (p.mRhs.get(0) == p.mLhs) { //�����ķ��Ҳ��ĵ�һ�����ս�����󲿵ķ��ս����ͬ���������ݹ�
						currentProds.remove(i);   //�ڵ�ǰ�ı��ʽ������ɾ������
						int T = mTerminalsNums + mNonTerminalsNums++;
						for (int j = currentProds.size() - 1; j >= 0; j--) {
							Prod pB = currentProds.get(j);
							if (pB.mRhs.get(0) == mLambda) {
								pB.mRhs.remove(0);
							}
							pB.mRhs.add(T);
						}
						ArrayList<Prod> prodT = new ArrayList<Prod>();
						mProductions.add(prodT);
						Prod pB = new Prod();
						pB.mLhs = T;        //��pB���󲿸�ֵΪT
						p.mRhs.remove(0);   //ɾ��������ݹ�Ĳ���ʽp���Ҳ���һ�������ݹ������
						pB.mRhs = p.mRhs;   //��pʣ�µ�����ݹ�Ĳ��ָ�ֵ��pB
						pB.mRhs.add(T);     //��pB����T��ֵ��pB�Ҳ������ұ�
						if (pB.mRhs.get(0) == mLambda) {
							pB.mRhs.remove(0);
						}
						prodT.add(pB);      
						Prod pC = new Prod();
						pC.mLhs = T;       //���ս����뵽��pB��ͬ�󲿵Ĳ���ʽ�Ҳ���
						pC.mRhs = new ArrayList<Integer>();
						pC.mRhs.add(mLambda);
						prodT.add(pC);     //��ݹ��������
						//System.out.println(prodT.toString());
						found = true;
						break;
					}
				}
			}
		} while (found);
	}

	/**
	 * ������ӹ���ǰ׺
	 * ˼·�ǻ����Ϊֱ�ӣ�Ȼ�����������к�������ֱ�ӹ���ǰ׺
	 */
	private void removeIndirectFactor() {
		boolean changes;
		do {
			changes = false;
			for (int i = mProductions.size() - 1; i >= 0; i--) {
				ArrayList<Prod> currentProds = mProductions.get(i);
				for (int j = currentProds.size() - 1; j >= 0; j--) {
					Prod pA = currentProds.get(j); //�õ�һ������ʽ
					int b = pA.mRhs.get(0);        //�õ�����ʽ�Ҳ��ĵ�һ������
					if (b >= mTerminalsNums) {     //����Ƿ��ս���ŵĻ�
						changes = true;
						ArrayList<Prod> prodsB = mProductions.get(b
								- mTerminalsNums); //�Ӳ���ʽ�б��еõ��������ʽ,�÷��Ŷ�Ӧ�Ĳ���ʽ����
						boolean allStartWithTerminals = true;
						for (int k = prodsB.size() - 1; k >= 0
								&& allStartWithTerminals; k--) {
							allStartWithTerminals = prodsB.get(k).mRhs.get(0) < mTerminalsNums; //�ҵ�����ĳ������ʽ��һ���ַ�Ϊ�ս����
						}
						if (!allStartWithTerminals)  //���allStartWithTerminalsΪ�棬˵������ǰ׺�����ս��
							continue;
						boolean hasLambda = false;
						for (int k = prodsB.size() - 1; k >= 0; k--) {
							Prod pB = prodsB.get(k); 
							if (pB.mRhs.get(0) == mLambda) { //����ò���ʽ��Ϊ��
								hasLambda = true;
							} else {                         //��Ϊ��
								Prod newProd = new Prod();   //
								newProd.mLhs = pA.mLhs;  
								newProd.mRhs = getSubList(pA.mRhs, 1,
										pA.mRhs.size());     //�൱��ȥ���˹���ǰ׺
								newProd.mRhs.addAll(0, pB.mRhs); //��0λ�ÿ�ʼ����pB���Ҳ�
								currentProds.add(newProd);
							}
						}
						if (hasLambda) {
							pA.mRhs.remove(0);
							if (pA.mRhs.size() == 0) {
								pA.mRhs.add(mLambda);
							}
						} else {
							currentProds.remove(j);
						}
					}
				}
			}
			if (changes) {
				factor();
				removeSameProds();
			}
		} while (changes);
	}

	/**
	 * ����Ԥ�������
	 */
	private void createPredictMap() {
		mPredictMap = new int[mNonTerminalsNums][];   //Ԥ���������һ����λ��������
		for (int i = 0; i < mNonTerminalsNums; i++) {
			mPredictMap[i] = new int[mTerminalsNums];
			Arrays.fill(mPredictMap[i], PREDICT_NULL);//�ֽ�����ֵ����ֵΪ-1����ʼ��
		}
		HashSet<Integer> hashSet = new HashSet<Integer>();
		for (int k = mProductions.size() - 1; k >= 0; k--) {
			ArrayList<Prod> currentProds = mProductions.get(k);
			for (int i = currentProds.size() - 1; i >= 0; i--) {
				hashSet.clear();
				Prod p = currentProds.get(i);
				computeFirst(hashSet, p.mRhs); //���ҵ�ĳ������ʽ��first����
				if (hashSet.contains(mLambda)) {//���first�����а����յĻ�
					hashSet.remove(mLambda);
					hashSet.addAll(mFollowSet[p.mLhs - mTerminalsNums]); //hashset�����follow����
				}
				for (int j = 0; j < mTerminalsNums; j++) {
					if (hashSet.contains(j)) {  //��ϣset�а�����first���Ϻ�follow���������ս����j
						mPredictMap[k][j] = i;  //����Ӧ�Ĳ���ʽ��ż��������У�֪��k��i���ɻ�ԭ����Ŷ�Ӧ�Ĳ���ʽ
					} else if (mPredictMap[k][j] == PREDICT_NULL 
							&& mFollowSet[k].contains(j)) {
						mPredictMap[k][j] = PREDICT_SYNCH;
					}
				}
			}
		}
	}
/**
 * ��ӡԤ�������
 * @param outputFileName
 * @param printProdNumber
 * @param printProdSentence
 * @param printNumber
 * @param printName
 */
	private void printPredictMap(String outputFileName,
			boolean printProdNumber, boolean printProdSentence,
			boolean printNumber, boolean printName) {
		try {
			FileOutputStream fout = new FileOutputStream(outputFileName);
			if (printProdSentence) {
				if (printName) {
					fout.write("\t".getBytes());
					for (int i = 0; i < mTerminalsNums; i++) {
						fout.write((getWordName(i) + "\t").getBytes());
					}
					fout.write("\r\n".getBytes());
					for (int i = 0; i < mNonTerminalsNums; i++) {
						fout.write((getWordName(i + mTerminalsNums) + "\t")
								.getBytes());
						for (int j = 0; j < mTerminalsNums; j++) {
							if (mPredictMap[i][j] > -1) {
								Prod p = mProductions.get(i).get( //����ʽ�Ļ�ԭ
										mPredictMap[i][j]);
								fout.write("->".getBytes());
								for (int k = 0; k < p.mRhs.size(); k++) {
									fout.write(getWordName(p.mRhs.get(k))
											.getBytes());
								}
							}
							fout.write("\t".getBytes());
						}
						fout.write("\r\n".getBytes());
					}
				}
			}
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * LL��������������
	 * 
	 * @throws IOException
	 */
	public void lldriver(String inputFileName, String outputFileName)
			throws IOException {
		Scanner cin = new Scanner(new File(inputFileName)); //�Ӵʷ���������ļ��еõ����������
		String t = cin.nextLine();
		String[] ts = t.split("\t");
		int a = Integer.parseInt(ts[2]);         //��Ӧ�ôʷ��ڴʷ������ֵ��е����
		String value = ts[4];                    
		int lineNumber = Integer.parseInt(ts[7]); //����Ŀ������Դ�������ڵ�����	
//		System.out.println("0:"+ts[0]);
//		System.out.println("1:"+ts[1]);
//		System.out.println("2:"+ts[2]);
//		System.out.println("3:"+ts[3]);
//		System.out.println("4:"+ts[4]);
//		System.out.println("5:"+ts[5]);
//		System.out.println("6:"+ts[6]);
//		System.out.println("7:"+ts[7]);
		boolean lastErrorHandled = false;
		FileOutputStream fout = new FileOutputStream(outputFileName);
		mGrammerTree = new GrammerAnalysisTree();//�½�һ���﷨�����������������ݽṹ���GrammerAnalysisTree��
		GrammerAnalysisTree.Node currentNode = mGrammerTree.mRoot; //���ڵ�
		currentNode.mSymbol = mStartSymbol; //���ս����ʼʱ����ֵ,��program
		Stack<GrammerAnalysisTree.Node> stack = new Stack<GrammerAnalysisTree.Node>();//����һ��GrammerAnalysisTree.Node���͵�ջ
		stack.push(currentNode);  //�����ڵ�ѹ��ջ
		while (!stack.empty()) {  //���ջ��Ϊ�վ�˵���﷨����δ����������Ҫһֱѭ��
			currentNode = stack.pop();
			int x = currentNode.mSymbol;  //��ǰ�ڵ�ı��
			if (x >= mTerminalsNums && mPredictMap[x - mTerminalsNums][a] > -1) {  //����x�൱�ڷ��ս������������Ԥ��������е�ֵ��Ϊ-1����û�г��ִ���
				int k = mPredictMap[x - mTerminalsNums][a];   //��Ԥ��������λ���ϵ�ֵȡ��,x - mTerminalsNums�������ʽ���ϵı��
				Prod p = mProductions.get(x - mTerminalsNums).get(k); //�õ���Ӧ�Ĳ���ʽ
				for (int i = p.mRhs.size() - 1; i >= 0; i--) {
					stack.push(currentNode.addSon(0, p.mRhs.get(i))); //�����Ҳ����൱�ڶ��ӽڵ㣬ѹ��ջ
				}
				lastErrorHandled = false;
				printProduction(fout, p, false, true); //��ӡ
			} else if (x == a) {  //����x��ֵ����Ӵʷ���������ļ��ж��������ֵ��ȵĻ�
				currentNode.mValue = value;
				lastErrorHandled = false;
				a = mLambda;
				value = "-";
				if (cin.hasNextLine()) {
					t = cin.nextLine();
					ts = t.split("\t");
					if (ts.length > 7) {
						a = Integer.parseInt(ts[2]);   //��a���¸�ֵ
						value = ts[4];                 //�õ���valueֵ��INT��CH(�ַ�����),char�ȵ�
						lineNumber = Integer.parseInt(ts[7]);//�õ���Ӧ���к�
					}
				}
			} else if (x == mLambda) { //���xΪ��
				lastErrorHandled = false;
			} else {
				String error;            //��������
				int number = lineNumber; //������ֵľ���λ��
				/**
				 * ����ָ�
				 */
				if (x >= mTerminalsNums
						&& mPredictMap[x - mTerminalsNums][a] == PREDICT_SYNCH) {
					/**
					 * ���M[A,a]��synch����ô����ͼ��������ʱ��ջ���ķ��ս���� ������(�������еĹ���synch�Ĺ涨)
					 */
					error = getWordName(a)+"֮ǰȱ�ٱ�Ҫ�ķ���";   //��ʾ����
				} else if (x >= mTerminalsNums
						&& mPredictMap[x - mTerminalsNums][a] == PREDICT_NULL) {
					error = "�������ķ���"+getWordName(a);      //��ʾ����
					/**
					 * ���M[A,a]�ǿգ��ͺ����������a
					 */
					a = mLambda;
					value = "-";
					if (cin.hasNextLine()) {
						t = cin.nextLine();
						ts = t.split("\t");
						if (ts.length > 7) {
							a = Integer.parseInt(ts[2]);
							value = ts[4];
							lineNumber = Integer.parseInt(ts[7]);
						
						}
					}
				} else if (x < mTerminalsNums) {
					error = "��������" + getWordName(x) + ",ȴ�õ��˲�������"+getWordName(a);
					/**
					 * ���ջ�����ս���ź�������Ų�ƥ�䣬���ջ�е����÷���
					 */
				}else{
					error = getWordName(a)+"ǰ����δԤ�ϵĴ���";
				}
				/**
				 * ������
				 */
				if (!lastErrorHandled) {
					mParserErrorList.add(new CompileError(number, error));
					fout.write((error + "\r\n").getBytes());
					currentNode.mFather.mSonList.remove(currentNode);
					lastErrorHandled = true;
				}
			}
		}
		fout.close();
	}

	public void solveGrammerAnalysisTree(
			String clearedTwiceParserResultFileName,
			String clearedTwiceGrammerTreeFileName) throws IOException {
		mGrammerTree.mRoot = clearGrammerTreeUnUsedNode(mGrammerTree.mRoot,
				false);
		mGrammerTree.mRoot = clearGrammerTreeUnUsedNode(mGrammerTree.mRoot,
				true);
		printMeasuredParserResult(clearedTwiceParserResultFileName);
		printGrammerAnalysisTree(clearedTwiceGrammerTreeFileName);
	}

	/**
	 * ����﷨����Ŀ�֦ ��ν�Ŀ�֦��ζ�Ŵ�֦�µ�����Ҷ�ӽڵ�ҪôΪ���ս����ҪôΪLambda
	 * 
	 * @param node
	 *            Ҫ�����֦
	 * @param full
	 *            �Ƿ�������֦����ν��������ζ�Ž�ֻ��һ��Ҷ�ӵ�֦���ɴ�Ҷ��
	 * @return ����������֦
	 */
	public GrammerAnalysisTree.Node clearGrammerTreeUnUsedNode(
			GrammerAnalysisTree.Node node, boolean full) {
		if (node.mSymbol < mTerminalsNums) {  //����ڵ����ս��ַ������Ҳ�Ϊ�յĻ��Ͳ���ɾ��
			if (node.mSymbol != mLambda) {
				return node;
			}
			return null;
		}
		for (int i = node.mSonList.size() - 1; i >= 0; i--) {//�ݹ����
			GrammerAnalysisTree.Node son = clearGrammerTreeUnUsedNode(
					node.mSonList.get(i), full);
			if (son == null) {
				node.mSonList.remove(i);
			} else {
				node.mSonList.set(i, son);
			}
		}
		if (node.mSonList.size() == 0) {
			return null;
		}
		if (full) {
			if (node.mSonList.size() == 1) {
				return node.mSonList.get(0);
			}
		}
		return node;
	}

	/**
	 * ����﷨���������ļ���
	 * 
	 * @param fileName
	 *            �ļ���
	 * @throws IOException
	 */
	public void printGrammerAnalysisTree(String fileName) throws IOException {
		FileOutputStream fout = new FileOutputStream(fileName);
		ArrayList<String> s = new ArrayList<String>();
		printNode(s, mGrammerTree.mRoot, 0, 3);
		for (int i = 0; i < s.size(); i++) {
			fout.write((s.get(i) + "\r\n").getBytes());
		}
		fout.close();
	}
	/**
	 * ��ӡÿ���ڵ�
	 * @param fout
	 * @param node
	 * @param floor
	 * @param spaceNum
	 * @return
	 */
	public int printNode(ArrayList<String> fout, GrammerAnalysisTree.Node node,int floor, int spaceNum){
		String s;
		if(fout.size() <= floor){
			s = "";			
			fout.add(s);
		}else{
			s = fout.get(floor);
		}
		while(s.length() < spaceNum){
			s += " ";
		}
		if(node.mSymbol < mTerminalsNums){ //���node���ս��ַ�
			s += "("+getWordName(node.mSymbol)+",["+node.mValue+"])";
			fout.set(floor, s);
			return s.length();
		}else{                            //���ս��ַ�
			int len;
			s += getWordName(node.mSymbol);
			fout.set(floor, s);
			len = s.length();
			if(node.mSonList.size() == 0){
				return len;
			}else {
				while(fout.size() < floor+4){
					fout.add("");
				}
				String ts = fout.get(floor+1);
				while(ts.length() < spaceNum+2){
					ts += " ";
				}
				ts += "|";
				fout.set(floor+1, ts);
				ts = fout.get(floor+2);
				
				int maxLen;
				int len1 = ts.length();
				maxLen = len1;
				if(fout.size() > floor + 4){
					ts = fout.get(floor + 4);
					int len2 = ts.length();
					maxLen = Math.max(maxLen, len2);
					if(node.mSonList.get(0).mSonList.size() > 0){
						if(fout.size() > floor + 6){
							ts = fout.get(floor+6);
							int len3 = ts.length();
							maxLen = Math.max(maxLen, len3);
						}
					}
				}
				ts = fout.get(floor+2);
				maxLen += 5;
				if(maxLen < spaceNum+2){
					while(ts.length()<maxLen){
						ts += " ";
					}
					ts += "+";
				}else{
					while(ts.length() < spaceNum+2){
						ts +=" ";
					}
					ts+="+";
					while(ts.length() < maxLen){
						ts+="-";
					}
					if(maxLen != spaceNum+2){
						ts+="+";
					}
				}
				
				fout.set(floor+2, ts);
				ts = fout.get(floor+3);
				while(ts.length() < maxLen){
					ts += " ";
				}
				ts += "|";
				fout.set(floor+3, ts);
				len = Math.max(len, printNode(fout, node.mSonList.get(0), floor+4, maxLen - 2));
				for(int i = 1 ; i < node.mSonList.size() ; i++){
						ts = fout.get(floor + 4);
						int len2 = ts.length();
						maxLen = len2;
						if(node.mSonList.get(i).mSonList.size() > 0){
							if(fout.size() > floor + 6){
								ts = fout.get(floor+6);
								int len3 = ts.length();
								maxLen = Math.max(maxLen, len3);
							}
						}
					maxLen += 5;
					
					ts = fout.get(floor+2);
					while(ts.length() < maxLen){
						ts += ts.length() == spaceNum+2?"+":"-";
					}
					ts+="+";
					fout.set(floor+2, ts);
					ts = fout.get(floor+3);
					while(ts.length() < maxLen){
						ts += " ";
					}
					ts+="|";
					fout.set(floor+3, ts);
					len = printNode(fout, node.mSonList.get(i), floor+4, maxLen - 2);
				}
				ts = fout.get(floor+2);
				while(ts.length() < spaceNum+2 ){
					ts += "-";
				}
				if(ts.length() == spaceNum+2){
					ts+="+";
					fout.set(floor+2, ts);
				}
				return len;
			}
		}
	}

	/**
	 * ����������﷨�������������ļ���
	 * 
	 * @param fileName
	 * @throws IOException
	 */
	public void printMeasuredParserResult(String fileName) throws IOException {
		FileOutputStream fout = new FileOutputStream(fileName);
		printEach(fout, mGrammerTree.mRoot);
		fout.close();
	}

	private void printEach(FileOutputStream fout, GrammerAnalysisTree.Node node)
			throws IOException {
		if (node.mSymbol >= mTerminalsNums) {
			fout.write((getWordName(node.mSymbol) + "\t->\t").getBytes());
			for (int i = 0; i < node.mSonList.size(); i++) {
				fout.write(getWordName(node.mSonList.get(i).mSymbol).getBytes());
			}
			fout.write("\r\n".getBytes());
			for (int i = 0; i < node.mSonList.size(); i++) {
				printEach(fout, node.mSonList.get(i));
			}
		}
	}

	/**
	 * �����﷨���������б�
	 * 
	 * @return
	 */
	public ArrayList<CompileError> getErrorList() {
		return mParserErrorList;
	}
}