package view;

import java.io.IOException;

import controller.LoginController;

/**
 * Class launching the messenger application (client-side).
 * @author etudiant
 *
 */
public class ChatMain {
	
	/**
	 * ContactListWindow shared by everyone.
	 */
	public static ContactListWindow clw;
	/**
	 * LoginWindow shared by everyone.
	 */
	public static LoginWindow lw;
	/**
	 * LoginController shared by everyone.
	 */
	public static LoginController lc;
	
	/**
	 * Takes no arguments.
	 * @param arg
	 * @throws IOException
	 */
	public static void main(String[] arg) throws IOException
	{
		clw = new ContactListWindow();
		lw = new LoginWindow();
		lc = new LoginController(lw);
		lw.lancerAffichage();
	}
}
