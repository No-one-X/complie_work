package zsy.cn;

import java.util.ArrayList;
import java.util.Collections;

/**
 * ��������
 * ������������
 */

public class Compiler {

	
	public static void main(String[] args) {
		//���дʷ�����
		NewLexical nl = new NewLexical("�����ļ�/�ʷ������ֵ�.txt");
		nl.Anaylise("�ʷ��������/�ʷ��������.txt");
		
		System.out.println("�ʷ������������ʷ����������<�ʷ��������/�ʷ��������.txt>��<�ʷ��������/���ű�.txt>��<�ʷ��������/������.txt>��.");
		//�﷨����
		System.out.println("�����ķ���...");
		Parser ps = new Parser("�����ļ�/�ʷ������ֵ�.txt","�����ļ�/ԭʼ�ķ�.txt","�﷨�������/�淶���ķ�.txt","�﷨�������/Ԥ�������.txt");
		System.out.println("�ķ����ؽ������������ķ���<�﷨�������/�淶���ķ�.txt>��Ԥ���������<�﷨�������/Ԥ�������.txt>��");
		System.out.println("�﷨������...");
		
		
		ps.driver("�ʷ��������/�ʷ��������.txt", "�﷨�������/�﷨���������δ����.txt","�﷨�������/�﷨����������������Σ�.txt","�﷨�������/�﷨���������������Σ�.txt");
		System.out.println("�﷨�����������﷨���������<�﷨�������>�µ��ļ���.");
		//����������ʹ����б�
		ArrayList<CompileError>errorList = new ArrayList<CompileError>();

		errorList.addAll(ps.getErrorList());
		System.out.println("������"+errorList.size() + "������");
		Collections.sort(errorList, new SortByLineNumber());		
		for(int i = 0 ; i < errorList.size() ; i++){
			System.out.println("Error " + (i+1)+":\t" + errorList.get(i));
		}
		System.out.println("�������.");
	}

}
