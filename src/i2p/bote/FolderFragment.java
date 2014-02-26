package i2p.bote;

import i2p.bote.folder.EmailFolder;
import android.os.Bundle;
import android.support.v4.app.ListFragment;

public class FolderFragment extends ListFragment {
    public static final String FOLDER_NAME = "folder_name";

    public static FolderFragment newInstance(String folderName) {
        FolderFragment f = new FolderFragment();
        Bundle args = new Bundle();
        args.putString(FOLDER_NAME, folderName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String folderName = getArguments().getString(FOLDER_NAME);
        EmailFolder folder = BoteHelper.getMailFolder(folderName);
    }
}
