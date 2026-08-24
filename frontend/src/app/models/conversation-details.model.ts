import { ConversationType } from './conversation-type.model';
import { User } from './user.model';

export interface ConversationDetails {
  id: number;
  type: ConversationType;
  title: string;
  otherUser: User | null;
  participants: User[];
  createdAt: string;
}
