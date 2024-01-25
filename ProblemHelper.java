import javax.swing.*;
import javax.swing.text.Keymap;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Scanner;

/**
 * 簡易エディタとコンパイル、実行コマンドの簡略化、標準入出力の管理によって
 * 競技プログラミングを支援します。
 * 簡易エディタでは Ctrl+Z でアンドゥ、 Ctrl+S で保存が行えます。<br/>
 * 入力ウィンドウでは、左側に入力し、Emit ボタンを押すことで文字列がプログラムに送信されます。
 * 送信されたデータは右側に表示されます。<br/>
 * 出力ウィンドウでは右側にプログラムが出力された内容が表示され、
 * 左側に文字列を入力して Check ボタンを押すことで出力内容が一致しているかどうかチェックすることができます。
 * このチェックでは区切り文字は無視され、読み取り可能な文字列のみを文字列として一致しているか判定します。
 * 一致している場合はボタンが緑に、不一致なら赤色に変化します。
 */
public class ProblemHelper{

	// エディタ
	private static SimpleTextEditor editor;
	// コンパイル、実行コマンド
	private static CompileReceiver receiver;

	public static void main(String[] args){
		// エディタの生成
		SwingUtilities.invokeLater(()->{
			editor = new SimpleTextEditor();
			editor.setVisible(true);
		});
		// コマンド入力ウィンドウの生成
		SwingUtilities.invokeLater(()->{
			receiver = new CompileReceiver();
			receiver.setVisible(true);
		});
	}

	/**
	 * 指定されたコマンドを元にコンパイルを実行します。
	 * @param args コンパイルコマンド
	 */
	private static void compile(String args){
		new Thread(()->{
			try{
				Process p = Runtime.getRuntime().exec(args);
				p.waitFor();
				System.out.println("---Standard Output---");
				Scanner out = new Scanner(p.getInputStream());
				while(out.hasNext())
					System.out.println(out.nextLine());
				System.out.println("---Standard Error---");
				Scanner err = new Scanner(p.getErrorStream());
				while(err.hasNext())
					System.out.println(err.nextLine());
				System.out.println("---------------------");
			}catch(Exception e){
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * 実行コマンドを元に実行します。
	 * @param args 実行コマンド
	 */
	private static void execute(String args){

		new Thread(()->new EasyTest().process(args)).start();
	}

	/**
	 * コンパイル、実行コマンドを入力するウィンドウを生成するクラスです。
	 */
	private static class CompileReceiver extends JFrame{
		// コンパイルコマンド入力部
		private final JTextField compileText;
		// 実行コマンド入力部
		private final JTextField executeText;

		/**
		 * ウィンドウ生成を行なうコンストラクタです。
		 */
		private CompileReceiver(){

			setTitle("Compile Execute Option");
			setSize(400,200);
			setDefaultCloseOperation(EXIT_ON_CLOSE);

			compileText = new JTextField(18);
			compileText.setEditable(true);

			JPanel panelC = new JPanel();
			panelC.add(new JLabel("Compile Option"));
			panelC.add(compileText);

			executeText = new JTextField(18);
			executeText.setEditable(true);

			JPanel panelE = new JPanel();
			panelE.add(new JLabel("Execute Option"));
			panelE.add(executeText);

			JButton buttonC = new JButton("Compile");
			buttonC.addActionListener(e->compile(compileText.getText()));

			JButton buttonE = new JButton("Execute");
			buttonE.addActionListener(e->execute(executeText.getText()));

			JPanel textPanel = new JPanel(new GridLayout(4,1));
			textPanel.add(buttonC);
			textPanel.add(panelC);
			textPanel.add(buttonE);
			textPanel.add(panelE);

			add(textPanel,BorderLayout.CENTER);
		}
	}

	/**
	 * 簡易エディタを生成するクラスです。
	 */
	private static class SimpleTextEditor extends JFrame{
		// 入力部
		private final JTextArea textArea;
		// 選択されたファイルを管理するインスタンス
		private final JFileChooser fileChooser;
		// タブ幅を管理するインスタンス
		private final JSpinner tabWidthSpinner;
		// フォントを管理するインスタンス
		private final JComboBox<String> fontComboBox;
		// フォントサイズを管理するインスタンス
		private final JSpinner fontSizeSpinner;
		// 現在開いているファイルのパス
		private File currentFile;
		// アンドゥ用のインスタンス
		private final UndoManager undoManager;

		/**
		 * エディタを生成するコンストラクタです。
		 */
		public SimpleTextEditor(){

			setTitle("Simple Text Editor");
			setSize(900,600);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			Font font = new Font("Monospaced",Font.PLAIN,16);
			Insets insets = new Insets(10,0,0,0);

			textArea = new JTextArea();
			textArea.setTabSize(8);
			textArea.setMargin(insets);
			textArea.setFont(font);
			textArea.setBackground(Color.BLACK);
			textArea.setForeground(Color.CYAN);
			textArea.setEditable(true);
			textArea.setCaretColor(Color.YELLOW);

			undoManager = new UndoManager();
			textArea.getDocument().addUndoableEditListener(undoManager);

			JScrollPane scrollPane = new JScrollPane(textArea);

			// 元々は行番号を表示するAreaでしたが、なんか表示が上手くいかないのでただの空白になってます
			JTextArea lineNumberArea = new JTextArea();
			lineNumberArea.setBackground(Color.BLACK);
			lineNumberArea.setColumns(1);
			lineNumberArea.setEditable(false);

			scrollPane.setRowHeaderView(lineNumberArea);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

			fileChooser = new JFileChooser();

			tabWidthSpinner = new JSpinner(new SpinnerNumberModel(8,1,20,1));
			tabWidthSpinner.addChangeListener(e->applyTabWidth());

			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			String[] fontNames = ge.getAvailableFontFamilyNames();

			fontComboBox = new JComboBox<>(fontNames);
			fontComboBox.setSelectedItem("Monospaced");
			fontComboBox.addActionListener(e->changeFont());

			fontSizeSpinner = new JSpinner(new SpinnerNumberModel(16,1,100,1));
			fontSizeSpinner.addChangeListener(e->changeFontSize());

			setLayout(new BorderLayout());
			add(createControlPanel(),BorderLayout.NORTH);
			add(scrollPane,BorderLayout.CENTER);

			Keymap keymap = textArea.getKeymap();

			KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z,KeyEvent.CTRL_DOWN_MASK);
			keymap.addActionForKeyStroke(undoKeyStroke,new AbstractAction(){
				@Override public void actionPerformed(ActionEvent e){
					undo();
				}
			});

			KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S,KeyEvent.CTRL_DOWN_MASK);
			keymap.addActionForKeyStroke(saveKeyStroke,new AbstractAction(){
				@Override public void actionPerformed(ActionEvent e){
					saveFile();
				}
			});
		}

		/**
		 * コントロールパネルを作成します。
		 * @return 生成されたJPanel
		 */
		private JPanel createControlPanel(){

			JButton openButton = new JButton("Open");
			openButton.addActionListener(e->openFile());

			JButton saveButton = new JButton("Save");
			saveButton.addActionListener(e->saveFile());

			JPanel controlPanel = new JPanel(new FlowLayout());
			controlPanel.setBackground(Color.WHITE);
			controlPanel.add(createSpinnerPanel("Tab Width:",tabWidthSpinner));
			controlPanel.add(createFontComboBoxPanel(fontComboBox));
			controlPanel.add(createSpinnerPanel("Font Size:",fontSizeSpinner));
			controlPanel.add(openButton);
			controlPanel.add(saveButton);

			return controlPanel;
		}

		/**
		 * 指定されたラベルとJSpinnerでJPanelを生成します。
		 * @param label ラベル
		 * @param spinner Panelに入れるJSpinner
		 * @return 生成されたJPanel
		 */
		private JPanel createSpinnerPanel(String label,JSpinner spinner){
			JPanel panel = new JPanel();
			panel.setBackground(Color.WHITE);
			panel.add(new JLabel(label));
			panel.add(spinner);
			return panel;
		}

		/**
		 * フォントのComboBoxからJPanelを生成するようのメソッドです。
		 * @param comboBox フォント用のJComboBox
		 * @return 生成されたJPanel
		 */
		private JPanel createFontComboBoxPanel(JComboBox<String> comboBox){
			JPanel panel = new JPanel();
			panel.setBackground(Color.WHITE);
			panel.add(new JLabel("Font:"));
			panel.add(comboBox);
			return panel;
		}

		/**
		 * Openボタンが押された時用のメソッドです。
		 */
		private void openFile(){

			int result = fileChooser.showOpenDialog(this);
			if(result==JFileChooser.APPROVE_OPTION){
				File file = fileChooser.getSelectedFile();
				try(BufferedReader reader = new BufferedReader(new FileReader(file))){
					StringBuilder content = new StringBuilder();
					String line;
					while((line = reader.readLine())!=null){
						content.append(line).append("\n");
					}
					textArea.setText(content.toString());
					setTitle("Simple Text Editor - "+file.getAbsolutePath());
					currentFile = file;
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}

		/**
		 * Saveボタンが押された時用のメソッドです。
		 * CtrlキーとSキーが押されたときもこのメソッドが呼び出されます。
		 */
		private void saveFile(){
			if(currentFile==null){
				int result = fileChooser.showSaveDialog(this);
				if(result==JFileChooser.APPROVE_OPTION){
					currentFile = fileChooser.getSelectedFile();
				}
				else{
					return;
				}
			}
			if(currentFile.exists()){
				int response = JOptionPane.showConfirmDialog(this,"Do you want to overwrite and save?","Attention",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
				if(response!=JOptionPane.YES_OPTION){
					return;
				}
			}
			try(BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))){
				writer.write(textArea.getText());
				setTitle("Simple Text Editor - "+currentFile.getAbsolutePath());
			}catch(IOException e){
				e.printStackTrace();
			}
		}

		/**
		 * タブ幅が変更された時用のメソッドです。
		 */
		private void applyTabWidth(){
			int tabWidth = (int)tabWidthSpinner.getValue();
			textArea.setTabSize(tabWidth);
		}

		/**
		 * フォントが変更された時用のメソッドです。
		 */
		private void changeFont(){
			String selectedFontName = (String)fontComboBox.getSelectedItem();
			Font currentFont = textArea.getFont();
			Font newFont = new Font(selectedFontName,currentFont.getStyle(),currentFont.getSize());
			textArea.setFont(newFont);
		}

		/**
		 * フォントサイズが変更された時用のメソッドです。
		 */
		private void changeFontSize(){
			int fontSize = (int)fontSizeSpinner.getValue();
			Font currentFont = textArea.getFont();
			Font newFont = new Font(currentFont.getName(),currentFont.getStyle(),fontSize);
			textArea.setFont(newFont);
		}

		/**
		 * CtrlキーとZキーが押された時用のメソッドです。
		 */
		private void undo(){
			try{
				if(undoManager.canUndo()){
					undoManager.undo();
				}
			}catch(CannotUndoException ex){
				ex.printStackTrace();
			}
		}
	}

	/**
	 * 実行用のウィンドウなどを管理するクラスです。
	 */
	private static class EasyTest extends Thread{
		// 出力先のウィンドウ
		private OutputWindow outputWindow;
		// 実行プログラムから読み込む用
		private BufferedReader br;
		// 実行プログラムのエラー出力を読み込む用
		private BufferedReader err;
		// 実行プログラムが
		private volatile boolean isAlive = true;

		/**
		 * 特に意味の無いコンストラクタ
		 */
		private EasyTest(){
		}

		/**
		 * プログラム実行用のメソッドです。
		 * @param args 実行コマンド
		 */
		public void process(String args){

			System.out.print("starting.");

			try{

				Process p = Runtime.getRuntime().exec(args);
				System.out.print(".");

				try(InputStream in = p.getInputStream();OutputStream out = p.getOutputStream()){

					isAlive = true;

					System.out.print(".");

					this.br = new BufferedReader(new InputStreamReader(in));
					this.err = new BufferedReader(new InputStreamReader(p.getErrorStream()));

					Thread t = new EasyTest();

					System.out.println("\ndone");

					new InputWindow(new PrintWriter(out,false));
					this.outputWindow = new OutputWindow();

					t.start();
					p.waitFor();

				}catch(Exception e){
					System.out.println();
					e.printStackTrace();
				}

			}catch(Exception e){
				System.out.println();
				e.printStackTrace();
			}

			this.isAlive = false;
		}

		/**
		 * プログラムからの出力待ちをします。
		 */
		public void run(){
			try{
				while(this.isAlive){
					if(this.br.ready()){
						while(this.br.ready()){
							this.outputWindow.emitText(this.br.readLine()+"\n");
						}
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}

			// 終了後は標準エラー出力を得て終了
			System.out.println("Program stopped");
			try{
				System.out.println("---Standard Error---");
				while(this.err.ready()){
					System.out.println(this.br.readLine());
				}
				System.out.println("--------------------");
			}catch(Exception e){
				e.printStackTrace();
			}

			// 出力内容を確認する、という意味でもウィンドウは生かしているので
			// 任意のタイミングで消してもらうよう促す
			System.out.println("Please close window");
		}

		/**
		 * 入力を渡すためのウィンドウを生成するクラスです。
		 */
		private static class InputWindow extends JFrame{
			// 入力エリア
			private final JTextArea textArea;
			// 送信済みエリア
			private final JTextArea pastTextArea;
			// 実行プログラムへ出力する用
			private final PrintWriter out;

			private InputWindow(PrintWriter out){

				this.out = out;

				this.setTitle("Input");
				this.setSize(600,400);

				this.textArea = new JTextArea();
				this.textArea.setEditable(true);

				this.pastTextArea = new JTextArea();
				this.pastTextArea.setBackground(Color.BLACK);
				this.pastTextArea.setForeground(Color.WHITE);
				this.pastTextArea.setEditable(false);

				JButton button = new JButton("Emit");
				button.addActionListener(e->this.emitText());

				JPanel controlPanel = new JPanel(new FlowLayout());
				controlPanel.add(button);

				JPanel textPanel = new JPanel(new GridLayout());

				JScrollPane scrollPane1 = new JScrollPane(this.textArea);
				scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

				JScrollPane scrollPane2 = new JScrollPane(this.pastTextArea);
				scrollPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

				textPanel.add(scrollPane1);
				textPanel.add(scrollPane2);

				this.add(controlPanel,BorderLayout.NORTH);
				this.add(textPanel,BorderLayout.CENTER);
				this.setVisible(true);
			}

			/**
			 * テキストエリアの文字列をプログラムに送信します。
			 */
			private void emitText(){
				String text = this.textArea.getText();
				this.out.print(text);
				this.out.flush();
				this.textArea.setText("");
				this.pastTextArea.append(text);
			}
		}

		/**
		 * 出力を確認するためのウィンドウを生成します。
		 */
		private static class OutputWindow extends JFrame{
			// 出力内容
			private final JTextArea textArea;
			// 確認したい文字列を入力する用
			private final JTextArea acTextArea;
			// Checkボタン用
			private final JButton button;

			private OutputWindow(){

				this.setTitle("Output");
				this.setSize(600,400);

				this.acTextArea = new JTextArea();
				this.acTextArea.setEditable(true);

				this.textArea = new JTextArea();
				this.textArea.setBackground(Color.BLACK);
				this.textArea.setForeground(Color.WHITE);
				this.textArea.setEditable(false);

				this.button = new JButton("Check");
				this.button.addActionListener(e->this.checkText());

				JPanel controlPanel = new JPanel(new FlowLayout());
				controlPanel.add(this.button);

				JPanel textPanel = new JPanel(new GridLayout());

				JScrollPane scrollPane1 = new JScrollPane(this.textArea);
				scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

				JScrollPane scrollPane2 = new JScrollPane(this.acTextArea);
				scrollPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

				textPanel.add(scrollPane2);
				textPanel.add(scrollPane1);

				this.add(controlPanel,BorderLayout.NORTH);
				this.add(textPanel,BorderLayout.CENTER);
				this.setVisible(true);
			}

			/**
			 * プログラムから得た文字列を追記します。
			 * @param text
			 */
			private void emitText(String text){
				this.textArea.append(text);
			}

			/**
			 * 内容が一致しているかチェックします。
			 */
			private void checkText(){
				if(this.isAC())
					this.button.setBackground(Color.GREEN);
				else
					this.button.setBackground(Color.RED);
			}

			/**
			 * 入力内容と出力内容が区切り文字を無視して一致しているかチェックします。
			 * @return 一致していればtrue、不一致ならfalse
			 */
			private boolean isAC(){

				Scanner sc1 = new Scanner(this.textArea.getText());
				Scanner sc2 = new Scanner(this.acTextArea.getText());

				while(sc2.hasNext()){
					if(!sc1.hasNext())
						return false;
					if(!sc1.next().equals(sc2.next()))
						return false;
				}

				return !sc1.hasNext();
			}
		}
	}
}
