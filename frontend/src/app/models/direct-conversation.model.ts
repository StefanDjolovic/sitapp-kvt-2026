import { User } from './user.model';

export interface DirectConversation {
  id: number;
  otherUser: User;
  createdAt: string;
}

export interface CreateDirectConversationRequest {
  currentUserId: number;
  otherUserId: number;
}
