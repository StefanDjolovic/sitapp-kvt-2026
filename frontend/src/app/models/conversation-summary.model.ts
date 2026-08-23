import { ConversationType } from './conversation-type.model';
import { Message } from './message.model';

export interface ConversationSummary {
  id: number;
  type: ConversationType;
  title: string;
  lastMessage: Message | null;
  unreadCount: number;
}
