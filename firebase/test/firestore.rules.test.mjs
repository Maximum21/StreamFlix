import { afterEach, before, beforeEach, after, describe, it } from 'node:test'
import { readFileSync } from 'node:fs'
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from '@firebase/rules-unit-testing'
import { doc, getDoc, setDoc, deleteDoc, writeBatch } from 'firebase/firestore'

let env

const profile = (overrides = {}) => ({
  name: 'Ana',
  avatarUrl: '',
  isKidsProfile: false,
  createdAt: 1000,
  updatedAt: 1000,
  ...overrides
})

const userDoc = (overrides = {}) => ({
  uid: 'alice',
  email: 'alice@example.com',
  displayName: 'Alice',
  createdAt: 1000,
  emailVerified: false,
  ...overrides
})

async function seedUser(uid = 'alice', profileCount = 0) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), `users/${uid}`), userDoc({ uid, profileCount }))
  })
}

async function seedProfile(uid, id, data = profile()) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), `users/${uid}/profiles/${id}`), data)
  })
}

function beforeEachProfile() {
  beforeEach(async () => {
    await seedUser('alice', 1)
    await seedProfile('alice', 'p1')
  })
}

const as = (uid) => env.authenticatedContext(uid).firestore()

// Mirrors the client transaction: write the profile and bump the count on the parent together
async function createProfile(db, uid, id, newCount, data = profile()) {
  const batch = writeBatch(db)
  batch.set(doc(db, `users/${uid}/profiles/${id}`), data)
  batch.set(doc(db, `users/${uid}`), { profileCount: newCount }, { merge: true })
  return batch.commit()
}

async function deleteProfile(db, uid, id, newCount) {
  const batch = writeBatch(db)
  batch.delete(doc(db, `users/${uid}/profiles/${id}`))
  batch.set(doc(db, `users/${uid}`), { profileCount: newCount }, { merge: true })
  return batch.commit()
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-streamflix',
    firestore: { rules: readFileSync('firestore.rules', 'utf8') }
  })
})

after(async () => {
  await env.cleanup()
})

afterEach(async () => {
  await env.clearFirestore()
})

describe('users', () => {
  it('denies unauthenticated access', async () => {
    await seedUser()
    const db = env.unauthenticatedContext().firestore()
    await assertFails(getDoc(doc(db, 'users/alice')))
    await assertFails(setDoc(doc(db, 'users/alice'), userDoc()))
  })

  it('lets the owner create and read their document', async () => {
    const db = as('alice')
    await assertSucceeds(setDoc(doc(db, 'users/alice'), userDoc()))
    await assertSucceeds(getDoc(doc(db, 'users/alice')))
  })

  it('denies reading or writing another user document', async () => {
    await seedUser('alice')
    const db = as('bob')
    await assertFails(getDoc(doc(db, 'users/alice')))
    await assertFails(setDoc(doc(db, 'users/alice'), userDoc(), { merge: true }))
  })

  it('denies creating a document whose uid is someone else', async () => {
    await assertFails(setDoc(doc(as('alice'), 'users/alice'), userDoc({ uid: 'mallory' })))
  })

  it('keeps uid and createdAt immutable', async () => {
    await seedUser('alice')
    const db = as('alice')
    await assertFails(setDoc(doc(db, 'users/alice'), { uid: 'bob' }, { merge: true }))
    await assertFails(setDoc(doc(db, 'users/alice'), { createdAt: 5 }, { merge: true }))
    await assertSucceeds(setDoc(doc(db, 'users/alice'), { emailVerified: true }, { merge: true }))
  })

  it('lets the owner delete their own document but nobody else', async () => {
    await seedUser('alice')
    await assertFails(deleteDoc(doc(as('bob'), 'users/alice')))
    await assertSucceeds(deleteDoc(doc(as('alice'), 'users/alice')))
  })
})

describe('profiles', () => {
  it('lets the owner create a valid profile', async () => {
    await seedUser('alice', 0)
    await assertSucceeds(createProfile(as('alice'), 'alice', 'p1', 1))
  })

  it('denies other users access to profiles', async () => {
    await seedUser('alice', 1)
    await seedProfile('alice', 'p1')
    await assertFails(getDoc(doc(as('bob'), 'users/alice/profiles/p1')))
  })

  it('denies an empty or over-long name', async () => {
    await seedUser('alice', 0)
    await assertFails(createProfile(as('alice'), 'alice', 'p1', 1, profile({ name: '' })))
    await assertFails(createProfile(as('alice'), 'alice', 'p1', 1, profile({ name: 'x'.repeat(21) })))
  })

  it('denies unknown fields', async () => {
    await seedUser('alice', 0)
    await assertFails(createProfile(as('alice'), 'alice', 'p1', 1, profile({ isAdmin: true })))
  })

  it('denies a sixth profile', async () => {
    await seedUser('alice', 5)
    await assertFails(createProfile(as('alice'), 'alice', 'p6', 6))
  })

  it('allows the fifth profile', async () => {
    await seedUser('alice', 4)
    await assertSucceeds(createProfile(as('alice'), 'alice', 'p5', 5))
  })

  it('keeps createdAt immutable on update', async () => {
    await seedUser('alice', 1)
    await seedProfile('alice', 'p1')
    const db = as('alice')
    await assertFails(setDoc(doc(db, 'users/alice/profiles/p1'), profile({ createdAt: 9 })))
    await assertSucceeds(setDoc(doc(db, 'users/alice/profiles/p1'), profile({ name: 'Anna' })))
  })

  it('denies deleting the last profile', async () => {
    await seedUser('alice', 1)
    await seedProfile('alice', 'p1')
    await assertFails(deleteProfile(as('alice'), 'alice', 'p1', 0))
  })

  it('allows deleting one of several profiles', async () => {
    await seedUser('alice', 2)
    await seedProfile('alice', 'p1')
    await seedProfile('alice', 'p2')
    await assertSucceeds(deleteProfile(as('alice'), 'alice', 'p1', 1))
  })
})

describe('user document shape', () => {
  it('rejects unknown fields on create and update', async () => {
    const db = as('alice')
    await assertFails(setDoc(doc(db, 'users/alice'), userDoc({ isAdmin: true })))
    await seedUser('alice')
    await assertFails(setDoc(doc(db, 'users/alice'), { notes: 'x'.repeat(10) }, { merge: true }))
  })

  it('rejects wrong types and oversized values', async () => {
    await seedUser('alice')
    const db = as('alice')
    await assertFails(setDoc(doc(db, 'users/alice'), { emailVerified: 'yes' }, { merge: true }))
    await assertFails(setDoc(doc(db, 'users/alice'), { displayName: 'x'.repeat(101) }, { merge: true }))
    await assertFails(setDoc(doc(db, 'users/alice'), { email: 'x'.repeat(321) }, { merge: true }))
  })

  it('accepts the exact document the app creates for an account that has no stored profile data', async () => {
    const db = as('alice')
    const batch = writeBatch(db)
    batch.set(doc(db, 'users/alice/profiles/p1'), profile())
    // Mirrors FirestoreProfileDataSource.userFields for a missing document (display name can be null)
    batch.set(doc(db, 'users/alice'), {
      uid: 'alice',
      email: 'alice@example.com',
      displayName: null,
      createdAt: Date.now(),
      emailVerified: true,
      profileCount: 1
    })
    await assertSucceeds(batch.commit())
  })

  it('still accepts the fields the app writes', async () => {
    await seedUser('alice')
    await assertSucceeds(
      setDoc(doc(as('alice'), 'users/alice'), { displayName: 'Alice', emailVerified: true }, { merge: true })
    )
  })
})

describe('accounts created before profile counts existed', () => {
  // The first profile on such an account must still be creatable: the document has no profileCount yet
  it('allows creating a profile when the user document has no profileCount', async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const { profileCount, ...legacy } = userDoc({ uid: 'alice' })
      await setDoc(doc(ctx.firestore(), 'users/alice'), legacy)
    })
    await assertSucceeds(createProfile(as('alice'), 'alice', 'p1', 1))
  })

  it('allows creating a profile when the user document does not exist yet', async () => {
    const db = as('alice')
    const batch = writeBatch(db)
    batch.set(doc(db, 'users/alice/profiles/p1'), profile())
    batch.set(doc(db, 'users/alice'), userDoc({ profileCount: 1 }))
    await assertSucceeds(batch.commit())
  })

  it('allows updating a user document that lacks uid and createdAt, as long as uid is not forged', async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'users/alice'), { email: 'alice@example.com' })
    })
    const db = as('alice')
    await assertSucceeds(setDoc(doc(db, 'users/alice'), { uid: 'alice', profileCount: 1 }, { merge: true }))
    await assertFails(setDoc(doc(db, 'users/alice'), { uid: 'mallory' }, { merge: true }))
  })
})

describe('account deletion', () => {
  it('allows deleting the last profile together with the user document', async () => {
    await seedUser('alice', 1)
    await seedProfile('alice', 'p1')
    const db = as('alice')
    const batch = writeBatch(db)
    batch.delete(doc(db, 'users/alice/profiles/p1'))
    batch.delete(doc(db, 'users/alice'))
    await assertSucceeds(batch.commit())
  })

  it('still denies deleting the last profile on its own', async () => {
    await seedUser('alice', 1)
    await seedProfile('alice', 'p1')
    await assertFails(deleteDoc(doc(as('alice'), 'users/alice/profiles/p1')))
  })

  it('does not let another user delete the data', async () => {
    await seedUser('alice', 1)
    await seedProfile('alice', 'p1')
    const db = as('bob')
    const batch = writeBatch(db)
    batch.delete(doc(db, 'users/alice/profiles/p1'))
    batch.delete(doc(db, 'users/alice'))
    await assertFails(batch.commit())
  })
})

const listItem = (overrides = {}) => ({
  movieId: '603',
  title: 'The Matrix',
  posterPath: '/p.jpg',
  addedAt: 1000,
  ...overrides
})

describe('my list', () => {
  beforeEachProfile()

  it('lets the owner add, read and remove a title', async () => {
    const db = as('alice')
    const ref = doc(db, 'users/alice/profiles/p1/list/603')
    await assertSucceeds(setDoc(ref, listItem()))
    await assertSucceeds(getDoc(ref))
    await assertSucceeds(deleteDoc(ref))
  })

  it('accepts a title without a poster', async () => {
    await assertSucceeds(
      setDoc(doc(as('alice'), 'users/alice/profiles/p1/list/603'), listItem({ posterPath: null }))
    )
  })

  it('requires the stored movie id to match the document id', async () => {
    await assertFails(setDoc(doc(as('alice'), 'users/alice/profiles/p1/list/603'), listItem({ movieId: '1' })))
  })

  it('refuses entries under a profile that does not exist', async () => {
    await assertFails(setDoc(doc(as('alice'), 'users/alice/profiles/ghost/list/603'), listItem()))
    await assertFails(setDoc(doc(as('alice'), 'users/alice/profiles/ghost/history/603'), historyItem()))
  })

  it('rejects unknown fields and missing titles', async () => {
    const ref = doc(as('alice'), 'users/alice/profiles/p1/list/603')
    await assertFails(setDoc(ref, listItem({ extra: true })))
    await assertFails(setDoc(ref, listItem({ title: '' })))
  })

  it('denies other users', async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'users/alice/profiles/p1/list/603'), listItem())
    })
    const db = as('bob')
    await assertFails(getDoc(doc(db, 'users/alice/profiles/p1/list/603')))
    await assertFails(setDoc(doc(db, 'users/alice/profiles/p1/list/604'), listItem({ movieId: '604' })))
  })
})

const historyItem = (overrides = {}) => ({
  movieId: '603',
  title: 'The Matrix',
  posterPath: '/p.jpg',
  backdropPath: null,
  watchedAt: 1000,
  positionMillis: 42000,
  ...overrides
})

describe('watch history', () => {
  beforeEachProfile()

  it('lets the owner record and read a watched trailer', async () => {
    const ref = doc(as('alice'), 'users/alice/profiles/p1/history/603')
    await assertSucceeds(setDoc(ref, historyItem()))
    await assertSucceeds(getDoc(ref))
  })

  it('rejects a negative position', async () => {
    await assertFails(
      setDoc(doc(as('alice'), 'users/alice/profiles/p1/history/603'), historyItem({ positionMillis: -1 }))
    )
  })

  it('denies other users', async () => {
    await assertFails(
      setDoc(doc(as('bob'), 'users/alice/profiles/p1/history/603'), historyItem())
    )
  })
})

describe('everything else', () => {
  it('is closed', async () => {
    const db = as('alice')
    await assertFails(setDoc(doc(db, 'movies/1'), { title: 'x' }))
    await assertFails(getDoc(doc(db, 'movies/1')))
  })
})
