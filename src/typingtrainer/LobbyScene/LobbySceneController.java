package typingtrainer.LobbyScene;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import typingtrainer.*;
import typingtrainer.PregameClientScene.PregameClientSceneController;
import typingtrainer.PregameServerScene.PregameServerSceneController;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;

/**
 * Created by Meow on 04.04.2017.
 */

public class LobbySceneController
{
	@FXML
	public GridPane pane;
	@FXML
	public TableView serversTable;
	@FXML
	public Label refreshLabel;
	@FXML
	public TextField nameTextfield;
	@FXML
	public PasswordField passPassfield;

	private static final double NANOSECONDS_IN_MILLISECOND = 1e+6;
	private static final int SEARCHING_TIMEOUT = 5000;
	private static final int CONNECTING_TIMEOUT = 10000;
	private long startSearchingTime;
	private boolean isSearching;

	private TableColumn nameColumn;
	private TableColumn ipColumn;
	private TableColumn passwordFlagColumn;
	private ObservableList<ServerInfo> servers = FXCollections.observableArrayList();
	private ServerSocket serverSocket; //Используется для получения информации о сервере

	public void initialize()
	{
		System.out.println("Сцена лобби готова!");
		nameColumn = buildTableColumn("Имя сервера", "name", 200);
		ipColumn = buildTableColumn("IP сервера", "ip", 200);
		passwordFlagColumn = buildTableColumn("Пароль", "passwordFlag", 100);
		Label ph = new Label("СЕРВЕРЫ НЕ НАЙДЕНЫ");
		ph.setStyle("-fx-font-size: 60px; \n-fx-effect: dropshadow( one-pass-box, white, 0, 0, 0, 0);");
		serversTable.setPlaceholder(ph);

		//servers.add(new ServerInfo("Some server", "192.193.194.195", "+"));
		serversTable.setItems(servers);
		serversTable.getColumns().addAll(nameColumn, ipColumn, passwordFlagColumn);

		serversTable.setRowFactory(arg ->
		{
			TableRow<ServerInfo> row = new TableRow<>();
			row.setOnMouseClicked(event ->
			{
				if (event.getClickCount() == 2 && (!row.isEmpty()))
				{
					if (nameTextfield.getText().trim().length() > 0)
					{
						ServerInfo rowData = row.getItem();
						connect(rowData.getIp(), passPassfield.getText());
					}
					else
					{
						Main.pushInfoScene("Укажите никнейм");
					}
				}
			});
			return row;
		});

		refreshServerList();

		try
		{
			nameTextfield.setText(InetAddress.getLocalHost().getHostName());
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
	}

	private TableColumn buildTableColumn(String text, String property, int prefWidth)
	{
		TableColumn t = new TableColumn(text);
		t.setCellValueFactory(new PropertyValueFactory<ServerInfo, String>(property));
		t.setPrefWidth(prefWidth);
		return t;
	}

	public void onBackClicked(MouseEvent mouseEvent)
	{
		stopSearching();
		try
		{
			Main.sceneManager.popScene();
		}
		catch (InvocationTargetException e)
		{
			System.out.println(e.getMessage());
		}
	}

	private void refreshServerList()
	{
		Platform.runLater(() -> refreshLabel.setDisable(true));
		servers.clear();
		new Thread(() ->
		{
			System.out.println("Обновление списка серверов");
			try
			{
				InetAddress address = InetAddress.getByName("230.1.2.3");
				MulticastSocket mcSocket = new MulticastSocket();
				String msg = InetAddress.getLocalHost().getHostAddress();
				DatagramPacket dgPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, address, 7913);
				mcSocket.send(dgPacket);
				mcSocket.close();

				serverSocket = new ServerSocket(7913);
				serverSocket.setSoTimeout(SEARCHING_TIMEOUT);
				startSearchingTime = System.nanoTime();
				isSearching = true;
				while (System.nanoTime() - startSearchingTime < SEARCHING_TIMEOUT * NANOSECONDS_IN_MILLISECOND && isSearching)
				{
					try (Socket s = serverSocket.accept())
					{

						ObjectInputStream in = new ObjectInputStream(s.getInputStream());
						final ServerInfo info = (ServerInfo) in.readObject();
						Platform.runLater(() -> servers.add(info));
						startSearchingTime = System.nanoTime();
						s.close();
					}
					catch (SocketTimeoutException e)
					{
						System.out.println("SocketTimeout Exception");
						//e.printStackTrace();
						isSearching = false;
					}
				}
			}
			catch (SocketException e)
			{
				System.out.println("Socket Exception");
				//e.printStackTrace();
			}
			catch (IOException e)
			{
				System.out.println("IO Exception");
				//e.printStackTrace();
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
			finally
			{
				stopSearching();
			}
			System.out.println("Обновление завершено");
			Platform.runLater(() -> refreshLabel.setDisable(false));
		}).start();
	}

	public void onCreateClicked(MouseEvent mouseEvent) throws IOException
	{
		if (nameTextfield.getText().trim().length() > 0)
		{
			stopSearching();
			PregameServerSceneController.setArg_serverName(nameTextfield.getText());
			PregameServerSceneController.setArg_serverPassword(passPassfield.getText());
			Parent pregameServerSceneFXML = FXMLLoader.load(Main.class.getResource("PregameServerScene/pregameServerScene.fxml"));
			ManagedScene pregameServerScene = new ManagedScene(pregameServerSceneFXML, Main.DEFAULT_SCREEN_WIDTH, Main.DEFAULT_SCREEN_HEIGHT, Main.sceneManager);
			pregameServerScene.getStylesheets().add("typingtrainer/PregameServerScene/style.css");
			Main.sceneManager.pushScene(pregameServerScene);
		}
		else
		{
			Main.pushInfoScene("Укажите никнейм");
		}
	}

	public void onJoinClicked(MouseEvent mouseEvent) throws IOException
	{
		if (!serversTable.getSelectionModel().isEmpty())
		{
			if (nameTextfield.getText().trim().length() > 0)
			{
				ServerInfo rowData = (ServerInfo)(serversTable.getSelectionModel().getSelectedItem());
				connect(rowData.getIp(), passPassfield.getText());
			}
			else
			{
				Main.pushInfoScene("Укажите никнейм");
			}
		}

		//Для быстрого теста
		/*
		Group root = new Group();
		ManagedScene gameScene = new ManagedScene(root, Main.DEFAULT_SCREEN_WIDTH, Main.DEFAULT_SCREEN_HEIGHT, Color.LIGHTBLUE, Main.sceneManager);
		GameSceneController controller = new GameSceneController(gameScene, new Socket(), Word.Languages.RU, 2, false);
		controller.setPlayerNames("Kek", "Pek");
		gameScene.getStylesheets().add("typingtrainer/GameScene/style.css");
		Main.sceneManager.pushScene(gameScene);
		*/
	}

	public void onRefreshClicked(MouseEvent mouseEvent)
	{
		refreshServerList();
	}

	private void stopSearching()
	{
		isSearching = false;
		try
		{
			serverSocket.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void connect(String IP, String password)
	{
		stopSearching();
		DataInputStream in;
		DataOutputStream out;
		try
		{
			Socket socket = new Socket(IP, 7914);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());

			String msg = InetAddress.getLocalHost().getHostAddress() + "|" + password;
			out.writeUTF(msg);

			msg = in.readUTF();
			System.out.println("\"" + msg + "\"");

			if (msg.equals(PregameServerSceneController.CONNECTION_ACCEPTED_MSG))
			{
				stopSearching();
				PregameClientSceneController.setArg_socket(socket);
				PregameClientSceneController.setArg_username(nameTextfield.getText());
				Parent pregameClientSceneFXML = FXMLLoader.load(Main.class.getResource("PregameClientScene/pregameClientScene.fxml"));
				ManagedScene pregameClientScene = new ManagedScene(pregameClientSceneFXML, Main.DEFAULT_SCREEN_WIDTH, Main.DEFAULT_SCREEN_HEIGHT, Main.sceneManager);
				pregameClientScene.getStylesheets().add("typingtrainer/PregameClientScene/style.css");
				Main.sceneManager.pushScene(pregameClientScene);
			}
			else if (msg.equals(PregameServerSceneController.CONNECTION_DECLINED_MSG))
			{
				socket.close();
				Main.pushInfoScene("Доступ не разрешён");
			}
		}
		catch (ConnectException e)
		{
			System.out.println("Connect Exception");
			//e.printStackTrace();
			Main.pushInfoScene("Подключение не удалось");
		}
		catch (IOException e)
		{
			System.out.println("IOException");
			//e.printStackTrace();
		}
	}
}