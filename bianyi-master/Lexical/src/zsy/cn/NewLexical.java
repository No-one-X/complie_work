package zsy.cn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

public class NewLexical {

	/**
	 * @param args
	 */
	private Word[] mWordList;//�����б�
	//private ArrayList<CompileError> mLexicalErrorList;//�ʷ������б�
	private ArrayList<Simbol> mSimbolList;//���ű�
	private ArrayList<ConstVariable> mConstVariableList;//������
	public NewLexical(String dictFileName){
		try {
			Scanner cin = new Scanner(new File(dictFileName));
			mWordList = new Word[cin.nextInt()];     //�����б�
			for (int i = 0; i < mWordList.length; i++) {
				mWordList[i] = new Word(cin.next(), cin.nextInt(), cin.next(),
						cin.nextBoolean(), cin.next(), cin.next());
				//System.out.println(mWordList[i].getS());
			}
			//mLexicalErrorList = new ArrayList<CompileError>();
			mSimbolList = new ArrayList<Simbol>(); //���ű�
			mConstVariableList = new ArrayList<ConstVariable>();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public  void Anaylise(String outputFileName) {
		String infile = "����.txt";
		try {
			FileInputStream f = new FileInputStream(infile);
			BufferedReader dr = new BufferedReader(new InputStreamReader(f));
			FileOutputStream fout = new FileOutputStream(outputFileName);
			int count = 0;
			int linenumber = 0;
			String line = "";
			String token = "";
			boolean flag = true;  //ע�ͻ��б�ʶ����,Ϊfalse���ǳ���ע�ͻ��У�����û��
			while ((line = dr.readLine()) != null) {
				linenumber++;
					char[] strLine = line.toCharArray();
					for (int i = 0; i < strLine.length; i++) {
						char ch = strLine[i];
						//String token = "";
						/**
						 * �����ֵ
						 */
						if (CheckDigital(ch) && flag) 
						{
							int state = 1;
							boolean isfloat = false;
							boolean ispower = false;
							boolean error = false;
							boolean errork = false;   //�Ƿ������ֿ�ͷ���Ǻ���ȴ��������ĸ
							String errortoken = "";
							while (ch != '\0' && (CheckDigital(ch) || ch == '.' || ch == 'e' || ch == '-')) {
								i++;
								if(CheckChar(strLine[i])){ //�������ֺ�������ĸ����ֱ������
									error = true;
									i--;
									while(true){
										errortoken = errortoken + strLine[i];
										i++;
										if(strLine[i]==';'){
											i++;
											errork = true;
											break;
										}
									}
									System.out.print("<"+errortoken+">"+"--wrong token!");
									System.out.println(" ���ֺ��治������ĸֱ������������");
								}
								i--;
								if(errork){ //������������Ĵ����ֱ������ѭ��
									break;
								}
								if (ch == '.' ){
									isfloat = true;
								}else if(ch == 'e'){
									ispower = true;	
								}
								int k;
								for (k = 1; k <= 6; k++) {
									char NFAstr[] = digitDFA[state].toCharArray();
									if (ch != '#'
											&& DigitNFA(ch, NFAstr[k])) {
										token += ch;
										state = k;
										break;
									}
								}
								if (k > 6)
									break;
								i++;
								if(i>=strLine.length)
									break;
								ch = strLine[i];
							}
							if(state == 2 && isfloat){
								System.out.println("<"+token+"> is wrong!����С������Ƿ�������");
								error = true;
							}else if(state == 4 && ispower){
								System.out.println("<"+token+"> is wrong!��e�ĺ��������С�-��������");
								error = true;
							}else if(state == 5 && ispower){
								System.out.println("<"+token+"> is wrong!��e-�ĺ�������������");
								error = true;
							}
							if(!error){
								System.out.println("<"+token+">");
								String fileover="";
								fileover = token+"$\t(\t" +81 + "\t,\t-\t)\tINUM\t" + linenumber + "\r\n";
								fout.write(fileover.getBytes());
							}
							--i; //ʹ����forѭ����i��Ϊԭ����ֵ����Ϊ���ϻ�Ҫ��+1
							token = "";
						}
						/**
						 * ����ַ�����
						 */
						 else if (ch == '\'' && flag) 
							{
								int state = 0;
								boolean mistake = false;
								String tokenprint = "";
								tokenprint += ch;
								while (state != 3) {
									i++;
									if(i>=strLine.length){
										mistake = true;
										System.out.println("�ַ�����Ϊ�������ر�����");
										break;
									}	
									ch = strLine[i];
									if (ch == '\0') { 
										mistake = true;
										System.out.println("�ַ�����Ϊ�������ر�����");
										break;
									}
									
									for (int k = 0; k < 4; k++) {
										char tmpstr[] = simconstNFA[state].toCharArray();
										if (CheckConstNFA(ch, tmpstr[k])) {   //�������״̬��ת�䣬��tokenprint��token���в���
											tokenprint += ch;                 // ����Ҫ����ģ���token���������
											if (k == 2 && state == 1) {
												if (CheckEscapeCharacter(ch)) // ��ת���ַ�
													token = token + '\\' + ch;
												else
													token += ch;
											} else if (k != 3 && k != 1)
												token += ch;
											state = k;
											break;
										}
									}
								}
								
									if (token.length() == 1 && !mistake) { //
										System.out.println(tokenprint+"<�ַ�����,"+token+">");
										String fileover="";
										fileover = tokenprint+"$\t(\t" +83 + "\t,\t-\t)\tCH\t" + linenumber + "\r\n";
										fout.write(fileover.getBytes());
									} else if (token.length() == 2 && !mistake) {
										if (CheckEscapeCharacter(token.charAt(1))
												&& token.charAt(0) == '\\') {
											System.out.println(tokenprint+"<ת���ַ�,"+token+">");
											String fileover="";
											fileover = tokenprint+"$\t(\t" +83 + "\t,\t-\t)\tCH\t" + linenumber + "\r\n";
											fout.write(fileover.getBytes());
										}
									} else if(token.length() == 2 && mistake){
										if (CheckEscapeCharacter(token.charAt(1))
												&& token.charAt(0) == '\\') {
											System.out.println(tokenprint+"<ת���ַ�,"+token+">"+"---wrong token");
										}
									} else if(token.length() == 1 && mistake){
										System.out.println(tokenprint+"<�ַ�����,"+token+">"+"---wrong token");
									}
								
								token = "";
							}
						/**
						 * ����ʶ���͹ؼ���
						 */
						 else if(CheckChar(ch) && flag){
							 do {
									token += ch;
									i++;
									if(i>=strLine.length)
										break;
									ch = strLine[i];
								} while (ch != '\0' && (CheckChar(ch) || CheckDigital(ch)));
								--i; 
								if (CheckKeywords(token.toString()))   // �ؼ���
								{
									System.out.println("<"+token+"--keywordws>");
									
									fout.write(CheckmWordList(token.toString(),linenumber).getBytes());
								} else                                 // ��ʶ��
								{
									count++;
									System.out.println("< id,"+count+">  :" +token);
									String fileover="";
									fileover = token+"$\t(\t" +1 + "\t,\t-\t)\tIDN\t" + linenumber + "\r\n";
									
									fout.write(fileover.getBytes());
								}
								token = "";
						 }
						/**
						 * ���ע�ͺͳ���
						 */
						 else if(ch=='/' && flag){
							 token = token + ch;
							 i++;
							 //�ȼ���Ƿ��ǳ���
							 if((strLine[i]!='*' && strLine[i]!='/')){
								 System.out.println("<"+token+">"); //�ǳ��ŵĻ�ֱ�����
								 token="";
								 i--;                              //ָ���λ
							 }
							
							 else 
								{
								 ch = strLine[i];
									if (ch == '*') {        //��/*���͵�ע��
										token += ch; 
										int state = 2;     //��ʱ�Ѿ��Ǵ���״̬ת��ͼ��2��״̬

										while (state != 4) {
											i++;           //ָ�������ǰ�ƶ�
											if(i==strLine.length-1 && strLine[i]!='/')
												flag = false; //�����ս�����ڱ���,��flag��ʶ��Ϊfalse
											if(i>=strLine.length) //���ȳ���������󳤶Ⱦ�ֹͣ 
												break;
											ch = strLine[i]; 
											
											for (int k = 2; k <= 4; k++) {
												char tmpstr[] = noteNFA[state]
														.toCharArray();
												if (CheckNoteDFA(ch, tmpstr[k],
														state)) { //����Ƿ���Խ�����Ӧ��״̬ת��
													token += ch;
													state = k;
													break;
												}
											}
										}
									}
									else if(ch == '/') //������//���͵�ע��
									{
										int index = line.lastIndexOf("//");
										String tmpstr=line.substring(index);//��//����ʼ�����е�ĩβ
										int tmpint = tmpstr.length();//����ָ����Ҫ����ƫ�ƶ���
										for(int k=0;k<tmpint;k++) 
										{
											i++; //��ָ���������ֵ�Ա��´ε�ѭ��
										}
										token = tmpstr;
									}
									if(flag){
										System.out.println(token+"ע�");
										token = "";
									}
									
								}
							 
						 }
						 /**
						  *  ̎����N��̖��������==����
						  */
						 else if (CheckOperation(ch) && flag)
							{
								token += ch;
								if(i<strLine.length-1)
								i++;
								if(CheckOperation(strLine[i]) && strLine[i-1]!='('){
								String doubleoperation = token + strLine[i];
								if( doubleoperation.equals(":=") || doubleoperation.equals("||") || doubleoperation.equals("&&") || doubleoperation.equals("<=") || doubleoperation.equals(">=") || doubleoperation.equals("!=") || doubleoperation.equals("==") || doubleoperation.equals("/=") || doubleoperation.equals("+=") || doubleoperation.equals("-=")
										|| doubleoperation.equals("*=") || doubleoperation.equals("++") || doubleoperation.equals("--") || doubleoperation.equals("**")){
									token = doubleoperation;
								}
								}else{
									i--;
								}
								System.out.println("<"+token+">");
								fout.write(CheckmWordList(token.toString(),linenumber).getBytes());
								token = "";
							}
						/**
						 * �����м��л��д��ڵ�/*����ע�ͣ������ע����һ��֮��
						 * û�н�������˵����һ�е�����Ӧȫ����ע�����ݣ�֪���ҵ���
						 * ��ƥ����Ҳ�Ϊֹ��flag��ֵ��Ϊflase���ҵ�֮���ٽ�����Ϊtrue
						 */
						 else if(!flag){ 
							 int index = line.lastIndexOf("*/");
							 if(index==-1){
								 String tmpstr = line.substring(0);
								 token = token + tmpstr;
							 }else{
								 String tmpstr = line.substring(0,index);
								 token = token + tmpstr+"*/";
								 System.out.println(token+"ע��");
								 flag = true;
								 token = "";
								 i = line.lastIndexOf("/");
							 }
						 }
						/**
						 * ̎���ַ�������
						 * */
						 else if (ch == '"')
							{
								String token1 = "";
								token1 += ch;
								int state = 0;
								boolean mistake = false;
								while (state != 3 ) {
									i++;
									if(i>=strLine.length-1) 
									{
										mistake = true;
										System.out.println("�ַ�������Ϊ�����ر�˫����");
										break;
									}
									ch = strLine[i];
									if (ch == '\0') {
										mistake = true;
										System.out.println("�ַ�������Ϊ�����ر�˫����");
										break;
									}
									for (int k = 0; k < 4; k++) {
										char tmpstr[] = constNFA[state].toCharArray();
										if (CheckStringDFA(ch, tmpstr[k])) {
											token1 += ch;
											if (k == 2 && state == 1) {  //public static String constNFA[] = { "#\\d#", "##a#", "#\\d\"", "####" };
												if (CheckEscapeCharacter(ch)) // ��ת���ַ�
													token = token + '\\' + ch;
												else
													token += ch;
											} else if (k != 3 && k != 1)
												token += ch;
											state = k;
											break;
										}
									}
								}
								if(mistake){
									System.out.println("<"+token+"> :"+token1+"--wrong token!" );
									token = "";
								}else{
									System.out.println("<"+token+"> :"+token1 );
									String fileover="";
									fileover = token+"$\t(\t" +1 + "\t,\t-\t)\tSTR\t" + linenumber + "\r\n";
									fout.write(fileover.getBytes());
									token = "";
								}
							}
				
			}
					}
			fout.close();
	}catch (IOException e) {
		e.printStackTrace();
	}
		
	}
	
	/**
	 * ����ַ��Ƿ�����ע�͵�״̬ת������
	 * @param ch
	 * @param key
	 * @param s
	 * @return
	 */
	public static boolean CheckNoteDFA(char ch, char key, int s) {
		if (s == 2) {
			if (key == 'c') {
				if (ch != '*')
					return true;
				else
					return false;
			}
		}
		if (s == 3) {
			if (key == 'c') {
				if (ch != '*' && ch != '/')
					return true;
				else
					return false;
			}
		}
		return ch == key;
	}
	/**
	 * ע�͵�״̬ת�Ʊ�
	 */
	public static String noteNFA[] = { "#####", "##*##", "##c*#", "##c*/", "#####" };
	/**
	 * ��ֵ״̬ת�Ʊ�
	 */
	public static String digitDFA[] = { "#######", "#d.#e##", "###d###", "###de##",
		"#####-d", "######d", "######d" };
	/**
	 * ����Ƿ�������
	 * @param ch
	 * @return
	 */
	public static Boolean CheckDigital(char ch) {
		return (ch >= '0' && ch <= '9');
	}

	/**
	 * ���ž���
	 */
	public static char operation[] = { ':','+', '-', '*', '=', '<', '>', '&', '|', '~',
		'^', '!', '(', ')', '[', ']', '{', '}', '%', ';', ',', '#', '.' };
	/**
	 * �ж��Ƿ�Ϊ���õķ���
	 * @param ch
	 * @return
	 */
	public static Boolean CheckOperation(char ch) 
	{
		for (int i = 0; i < 22; i++)
			if (ch == operation[i]) {
				return true;
			}
		return false;
	}
	/**
	 * ����Ƿ�������ֵ��״̬ת������
	 * @param ch
	 * @param key
	 * @return
	 */
	public static boolean DigitNFA(char ch, char key) {
		if (key == 'd') {
			if (CheckDigital(ch))
				return true;
			else
				return false;
		}
		return ch == key;
	}
	/**
	 * ����Ƿ�Ϊת���ַ�
	 * @param ch
	 * @return
	 */
	public static boolean CheckEscapeCharacter(char ch) {
		return ch == 'a' || ch == 'b' || ch == 'f' || ch == 'n' || ch == 'r'
				|| ch == 't' || ch == 'v' || ch == '?' || ch == '0';
	}
	/**
	 * ����Ƿ������ַ�������״̬ת��Ҫ��
	 * @param ch
	 * @param key
	 * @return
	 */
	public static boolean CheckConstNFA(char ch, char key) {
		if (key == 'a') //
			return true;
		else if (key == '\\' || key == '\'')
			return ch == key;
		else if (key == 'd')
			return ch != '\\' && ch != '\'';
		return false;
	}
	/**
	 * �ַ���������״̬ת�Ʊ�
	 */
	public static String constNFA[] = { "#\\d#", "##a#", "#\\d\"", "####" };
	/**
	 * �ַ��ַ�������״̬ת�Ʊ�
	 */
	public static String simconstNFA[] = {"#\\d#","##a#","###\'","####"};
	/**
	 * �ؼ��־���
	 */
	public static String keywords[] = { "auto", "double", "int", "struct",
		"break", "else", "long", "switch", "case", "enum", "register",
		"typedef", "char", "extern", "return", "union", "float",
		"short", "unsigned", "continue", "for", "signed", "void",
		"default", "goto", "sizeof", "volatile", "do", "if", "while",
		"static" ,"String"};
	/**
	 * 
	 * @param ch
	 * @return boolean
	 * ����Ƿ�Ϊ���ɱ�ʶ�����߹ؼ��ֵ��ַ�
	 */
	public static boolean CheckChar(char ch){
		if((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_'){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * 
	 * @param str
	 * @return boolean
	 * ����Ƿ�Ϊ�ؼ���
 	 */
	public static boolean CheckKeywords(String str){
		for(int i=0;i<keywords.length;i++){
			if(str.equals(keywords[i]))
				return true;
		}
		return false;
	}
	/**
	 * 
	 * @param ch
	 * @param key
	 * @return boolean
	 * ����ַ����Ƿ�������Ӧ��״̬�Ա����״̬ת��
	 */
	public static boolean CheckStringDFA(char ch, char key) {
		if (key == 'a')
			return true;
		else if (key == '\\' || key == '"')
			return ch == key;
		else if (key == 'd')
			return ch != '\\' && ch != '"';
		return false;
	}
	
	public String CheckmWordList(String word,int linenumber){
		String fileover = "";
		for(int i=0;i<mWordList.length;i++){
			if(word.equals(mWordList[i].getToken())){
				fileover = word+"$\t(\t" +mWordList[i].getType() + "\t,\t-\t)\t"+mWordList[i].getToken()+"\t" + linenumber + "\r\n";
				System.out.println(fileover);
				break;
		}
	}
		return fileover;
}
}
				
