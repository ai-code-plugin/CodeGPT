package ee.carlrobert.chatgpt.ide.conversations;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import ee.carlrobert.chatgpt.client.ClientCode;
import ee.carlrobert.chatgpt.client.ClientFactory;
import ee.carlrobert.chatgpt.ide.conversations.converter.ConversationConverter;
import ee.carlrobert.chatgpt.ide.conversations.converter.ConversationsConverter;
import ee.carlrobert.chatgpt.ide.settings.SettingsState;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "ee.carlrobert.chatgpt.ide.conversations.ConversationsState",
    storages = @Storage("ChatGPTConversations.xml")
)
public class ConversationsState implements PersistentStateComponent<ConversationsState> {

  @OptionTag(converter = ConversationsConverter.class)
  public ConversationsContainer conversationsContainer = new ConversationsContainer();

  @OptionTag(converter = ConversationConverter.class)
  public Conversation currentConversation;

  public static ConversationsState getInstance() {
    return ApplicationManager.getApplication().getService(ConversationsState.class);
  }

  @Nullable
  @Override
  public ConversationsState getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull ConversationsState state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public void setCurrentConversation(@Nullable Conversation conversation) {
    this.currentConversation = conversation;
  }

  public static @Nullable Conversation getCurrentConversation() {
    return getInstance().currentConversation;
  }

  public Conversation createConversation(ClientCode clientCode) {
    var settings = SettingsState.getInstance();
    var conversation = new Conversation();
    conversation.setId(UUID.randomUUID());
    conversation.setClientCode(clientCode);
    if (!settings.isChatGPTOptionSelected) {
      if (settings.isChatCompletionOptionSelected) {
        conversation.setModel(settings.chatCompletionBaseModel);
      } else {
        conversation.setModel(settings.textCompletionBaseModel);
      }
    }
    conversation.setCreatedOn(LocalDateTime.now());
    conversation.setUpdatedOn(LocalDateTime.now());
    return conversation;
  }

  public void addConversation(Conversation conversation) {
    var conversationsMapping = conversationsContainer.getConversationsMapping();
    var conversations = conversationsMapping.get(conversation.getClientCode());
    if (conversations == null) {
      conversations = new ArrayList<>();
    }
    conversations.add(conversation);
    conversationsMapping.put(conversation.getClientCode(), conversations);
  }

  public void saveConversation(Conversation conversation) {
    conversation.setUpdatedOn(LocalDateTime.now());
    var iterator = conversationsContainer.getConversationsMapping()
        .get(conversation.getClientCode())
        .listIterator();
    while (iterator.hasNext()) {
      var next = iterator.next();
      if (next.getId().equals(conversation.getId())) {
        iterator.set(conversation);
      }
    }
    setCurrentConversation(conversation);
  }

  public Conversation startConversation() {
    var conversation = createConversation(new ClientFactory().getClient().getCode());
    setCurrentConversation(conversation);
    addConversation(conversation);
    return conversation;
  }
}
