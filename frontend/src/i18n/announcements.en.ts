export const announcementEn: Record<string, { title: string; content: string }> = {
  'topic-auto-claim-gold': {
    title: 'Quests: unclaimed gold is collected at 23:59',
    content: 'At 23:59 Beijing time, daily gold you already earned this month but did not claim is collected. The current weekly quest and makeup gold are collected the same way. The gold counts on the day you played. Rest days, days past the quota, and expired weekly quests are not paid.',
  },
  'topic-daily-makeup': {
    title: 'Quests: makeup does not count for the weekly quest',
    content: 'Makeup does not count toward weekly quest progress. The weekly quest only counts teammates from each day’s first 3 matches. Makeup starts after those 3 and does not add a teammate. By Beijing time, the first day of the month has nothing to make up. From the next day, makeup is always the most recent unfinished day. If that day was played but not finished, the number left is the matches missed. If that day was not played, the notice says the month was played normally and nothing is owed only when normal play will still fill this month’s daily quests; that day can still be made up. If the daily quests cannot be filled, those matches are still owed. After one makeup, 2 are left. Makeup can be checked only after today’s 3 matches are done. On the last day of the month, the remaining matches of the day you are making up can be finished the same day. Gold already paid is not recalculated. The quest list shows Makeup match N and Makeup win: slots 1 / 2 / 3 pay 30 / 40 / 50 when claimed, plus 10 for a win, and the gold follows the slot you are making up. One makeup a day, except that month-end continuation. A forfeit spends that slot for 0 gold and blocks another makeup today. A void does not, so you can start again. Debt does not cross months. A day that already paid gold still pays when the work-day quota is full. A day that never paid needs a free work day, or the makeup pays nothing.',
  },
  'topic-customer-trigger-fx': {
    title: 'Battle: each customer trigger has its own effect',
    content: 'From round 2, a customer judgment lights the portrait, sends a signature object to whoever is actually affected, and floats the real number. The corner line becomes a short phrase. If nothing happens, the portrait shakes and shows Held. If a card says the customer cannot act, the portrait dims and shows Can\'t act. Round 1 is not judged.\n\nKind Steady: a gold ribbon to the Bully\'s attack, ATK −1. Timid: a pale puff into the HP bar, HP +the rolled amount (3 to 6). Anxious: a red spike to the attack, ATK +the rolled amount (2 to 4). Strolling Pair: two green arcs to the living guards, +1 each. Wealthy: a coin to the chosen guard, Call +1. Harsh: an ink blot on the chosen guard, Call −1, then HP −3. Shield does not block it. Ready Guest: a card to each living guard, draw 1 each.',
  },
  'topic-clement-customer': {
    title: 'Customers: Ready Guest walks in smiling',
    content: 'Ready Guest joins the gallery: an easygoing Singapore builder who says OK with a smile. In a normal match the seven customers are equally likely. If you draw this one, half of the rounds each living guard draws 1 card. A downed guard does not draw. The pressure list still picks only from Timid, Anxious, and Harsh.\n\nProtect the customer and take the Bully to 0 HP to win. Every customer shares the Hard Bully.',
  },
  'topic-november-workdays-21': {
    title: 'Attendance: 21 paid days in November',
    content: 'In November 2026 each player can claim gold on 21 work days. After that, the rest of the month is a rest day: daily and weekly quests pay nothing, and matches can still be played.',
  },
  'topic-daily-days-left': {
    title: 'Difficulty: days you cannot play are left out of the forecast',
    content: 'Daily gold in the forecast, like weekly gold, drops what there is no time to play. It uses the smaller of the days left until month end and the quota still unplayed, at 150 for a full win each. Work days already missed are not forecast. Weekly rules are unchanged.',
  },
  'topic-pressure-locked-at-midnight': {
    title: 'Difficulty: the pressure list locks at midnight',
    content: 'The projected top five lock at 00:00 by forecast gold, and that list is the only pressure list for the day. Matches and claims during the day no longer move people in or out. The forecast is unchanged: gold in hand + remaining work days × 150 + weekly quests still claimable this month. If a room has one of those five, the customer is drawn evenly from Timid, Anxious, and Harsh.',
  },
  'topic-weekly-windows-left': {
    title: 'Difficulty: expired weekly quests leave the forecast',
    content: 'A weekly quest that ended unclaimed cannot be caught up, and it is no longer in the forecast. Only weeks still open from this week through month end count, 500 each. At most 4 a month. This week still counts if it is unclaimed.',
  },
}
