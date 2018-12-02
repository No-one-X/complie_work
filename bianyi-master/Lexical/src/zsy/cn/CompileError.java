package zsy.cn;

import java.util.Comparator;
/**
 * ������
 * ���д�����кźʹ���ľ�����Ϣ��ʾ
 * 
 *
 */
public class CompileError {
	public int LineNumber;//�к�
	public String ErrorMessage;//��ʾ��Ϣ

	public CompileError(int lineNumber, String errorMessage) {
		LineNumber = lineNumber;
		ErrorMessage = errorMessage;
	}

	public String toString() { //��ӡ��ʾ��Ϣ
		return "Line " + LineNumber + ":\t" + ErrorMessage;
	}
}
//�����кŽ��������һ���ӿ�
class SortByLineNumber implements Comparator<CompileError> {

	@Override
	public int compare(CompileError o1, CompileError o2) {
		if (o1.LineNumber > o2.LineNumber) {
			return 1;
		} else {
			if (o1.LineNumber < o2.LineNumber) {
				return -1;
			}
		}
		return 0;
	}
}