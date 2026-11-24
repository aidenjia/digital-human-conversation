package con.aidenjia.digitalhuman.conversation.api.convoai.dto;

public enum DigitalHumanType {
    UE5,
    Unity,
    MMD;

    public static final String INVALID_VALUE_MESSAGE =
            "digitalHumanType must be UE5, Unity, or MMD";
}
