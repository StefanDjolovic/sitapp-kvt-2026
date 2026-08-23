import { User } from './user.model';

export interface Message {
  id: number;
  conversationId: number;
  sender: User;
  content: string;
  sentAt: string;
}
