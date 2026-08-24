import { ConversationType } from './conversation-type.model';
import { Message } from './message.model';
import { User } from './user.model';

export interface ConversationSummary {
  id: number;
  type: ConversationType;
  title: string;
  otherUser: User | null;
  lastMessage: Message | null;
  unreadCount: number;
}
