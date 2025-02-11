/*
 * RawDesfireFile.kt
 *
 * Copyright (C) 2011 Eric Butler
 * Copyright (C) 2019 Google
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 * Vladimir Serbinenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.card.desfire.files

import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class RawDesfireFile (
        val settings: ImmutableByteArray?,
        val data: ImmutableByteArray?,
        @Optional
        val error: String? = null,
        @Optional
        @XMLId("unauthorized")
        val isUnauthorized: Boolean = false,
        @Optional
        val readCommand: Byte? = null)